package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.common.ListPager;
import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.BatchResponse;
import com.pharmacy.demo.dto.response.MovementResponse;
import com.pharmacy.demo.entity.GoodsReceipt;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.Supplier;
import com.pharmacy.demo.entity.SystemSetting;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.BatchMapper;
import com.pharmacy.demo.mapper.MovementMapper;
import com.pharmacy.demo.repository.GoodsReceiptRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.StockMovementRepository;
import com.pharmacy.demo.repository.SupplierRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.BatchQueryService;
import com.pharmacy.demo.service.StockQueryService;
import com.pharmacy.demo.service.SystemSettingService;
import com.pharmacy.demo.util.TextSearch;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BatchQueryServiceImpl implements BatchQueryService {

    private static final Map<String, Comparator<BatchResponse>> SORTABLE = Map.of(
            "code", ListPager.text(BatchResponse::code),
            "medicineName", ListPager.text(BatchResponse::medicineName),
            "supplierName", ListPager.text(BatchResponse::supplierName),
            "receivedDate", ListPager.comparable(BatchResponse::receivedDate),
            "expiryDate", ListPager.comparable(BatchResponse::expiryDate),
            "initialQty", ListPager.comparable(BatchResponse::initialQty),
            "remainingQty", ListPager.comparable(BatchResponse::remainingQty),
            "status", ListPager.comparable(BatchResponse::status));

    private final StockBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final MedicineRepository medicineRepository;
    private final SupplierRepository supplierRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final StockQueryService stockQueryService;
    private final SystemSettingService settingService;
    private final BatchMapper batchMapper;
    private final MovementMapper movementMapper;
    private final Clock clock;

    @Override
    public PageResponse<BatchResponse> search(BatchFilter filter, PageQuery query, Actor actor) {
        boolean salesView = actor.role() == Role.SALES;
        LocalDate today = LocalDate.now(clock);
        List<StockBatch> candidates = batchRepository.findAll().stream()
                .filter(batch -> !salesView || batch.getRemainingQty() > 0)
                .filter(batch ->
                        filter.medicineId() == null || filter.medicineId().equals(batch.getMedicineId()))
                .filter(batch ->
                        filter.supplierId() == null || filter.supplierId().equals(batch.getSupplierId()))
                .filter(batch -> !filter.hasStock() || batch.getRemainingQty() > 0)
                .filter(batch -> filter.expiryWithin() == null || expiresWithin(batch, today, filter.expiryWithin()))
                .toList();
        List<BatchResponse> items = toResponses(candidates, salesView).stream()
                .filter(response -> filter.status() == null || filter.status() == response.status())
                .filter(response -> TextSearch.matchesAny(
                        filter.q(),
                        response.code(),
                        response.medicineName(),
                        response.medicineCode(),
                        response.supplierName(),
                        response.receiptCode()))
                .toList();
        return ListPager.paginate(items, query, SORTABLE, "expiryDate,asc");
    }

    @Override
    public BatchResponse get(String id) {
        StockBatch batch =
                batchRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy lô thuốc."));
        List<MovementResponse> movements = movementRepository.findByBatchIdOrderByCreatedAtDesc(id).stream()
                .map(movementMapper::toResponse)
                .toList();
        return BatchResponse.withMovements(toResponses(List.of(batch), false).get(0), movements);
    }

    @Override
    public List<BatchResponse> toResponses(List<StockBatch> batches, boolean salesView) {
        if (batches.isEmpty()) {
            return List.of();
        }
        SystemSetting settings = settingService.current();
        Map<String, Medicine> medicines =
                byId(medicineRepository.findAllById(distinct(batches, StockBatch::getMedicineId)), Medicine::getId);
        Map<String, String> supplierNames =
                supplierRepository.findAllById(distinct(batches, StockBatch::getSupplierId)).stream()
                        .collect(Collectors.toMap(Supplier::getId, Supplier::getName));
        Map<String, String> receiptCodes =
                receiptRepository.findAllById(distinct(batches, StockBatch::getReceiptId)).stream()
                        .collect(Collectors.toMap(GoodsReceipt::getId, GoodsReceipt::getCode));
        return batches.stream()
                .map(batch -> {
                    Medicine medicine = medicines.get(batch.getMedicineId());
                    BatchResponse response = batchMapper.toResponse(
                            batch,
                            stockQueryService.batchStatusOf(batch, settings),
                            medicine == null ? "" : medicine.getCode(),
                            medicine == null ? "" : medicine.getName(),
                            medicine == null ? "" : medicine.getUnit(),
                            supplierNames.getOrDefault(batch.getSupplierId(), ""),
                            receiptCodes.getOrDefault(batch.getReceiptId(), ""));
                    return salesView ? response.withoutCost() : response;
                })
                .toList();
    }

    private static boolean expiresWithin(StockBatch batch, LocalDate today, int days) {
        return batch.getRemainingQty() > 0
                && batch.getExpiryDate() != null
                && !batch.getExpiryDate().isBefore(today)
                && !batch.getExpiryDate().isAfter(today.plusDays(days));
    }

    private static List<String> distinct(Collection<StockBatch> batches, Function<StockBatch, String> extractor) {
        return batches.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static <T> Map<String, T> byId(Iterable<T> values, Function<T, String> idOf) {
        Map<String, T> result = new HashMap<>();
        values.forEach(value -> result.put(idOf.apply(value), value));
        return result;
    }

    @Override
    public List<BatchResponse> expiringSoon(int limit) {
        LocalDate warningLimit =
                LocalDate.now(clock).plusDays(settingService.current().getExpiryWarningDays());
        List<StockBatch> batches = batchRepository.findAll().stream()
                .filter(batch ->
                        batch.getRemainingQty() > 0 && !batch.getExpiryDate().isAfter(warningLimit))
                .sorted(Comparator.comparing(StockBatch::getExpiryDate))
                .limit(limit)
                .toList();
        return toResponses(batches, false);
    }
}
