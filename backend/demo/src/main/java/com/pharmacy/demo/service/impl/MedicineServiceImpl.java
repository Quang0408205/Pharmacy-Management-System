package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.common.ListPager;
import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.BatchResponse;
import com.pharmacy.demo.dto.response.MedicineResponse;
import com.pharmacy.demo.dto.response.MovementResponse;
import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.SystemSetting;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.enums.StockStatus;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.MedicineMapper;
import com.pharmacy.demo.mapper.MovementMapper;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.repository.StockBatchRepository;
import com.pharmacy.demo.repository.StockMovementRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.BatchQueryService;
import com.pharmacy.demo.service.MedicineService;
import com.pharmacy.demo.service.StockQueryService;
import com.pharmacy.demo.service.StockSummary;
import com.pharmacy.demo.service.SystemSettingService;
import com.pharmacy.demo.util.TextSearch;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements MedicineService {

    private static final String DEFAULT_SORT = "name,asc";
    private static final Map<String, Comparator<MedicineResponse>> SORTABLE = Map.of(
            "code", ListPager.text(MedicineResponse::code),
            "name", ListPager.text(MedicineResponse::name),
            "categoryName", ListPager.text(MedicineResponse::categoryName),
            "price", ListPager.comparable(MedicineResponse::price),
            "totalQty", ListPager.comparable(MedicineResponse::totalQty),
            "availableQty", ListPager.comparable(MedicineResponse::availableQty),
            "nearestExpiry", ListPager.comparable(MedicineResponse::nearestExpiry),
            "status", ListPager.comparable(MedicineResponse::status),
            "stockStatus", ListPager.comparable(MedicineResponse::stockStatus));

    private final MedicineRepository medicineRepository;
    private final CategoryRepository categoryRepository;
    private final StockBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final StockQueryService stockQueryService;
    private final SystemSettingService settingService;
    private final MedicineMapper medicineMapper;
    private final BatchQueryService batchQueryService;
    private final MovementMapper movementMapper;

    @Override
    public PageResponse<MedicineResponse> search(MedicineFilter filter, PageQuery pageQuery, Actor actor) {
        SystemSetting settings = settingService.current();
        Map<String, StockSummary> stock = stockQueryService.summarizeAll();
        Map<String, String> categoryNames = categoryNames();
        boolean salesView = actor.role() == Role.SALES;
        boolean activeOnly = salesView || filter.sellable();
        List<MedicineResponse> items = medicineRepository.findAll().stream()
                .filter(medicine -> !activeOnly || medicine.getStatus() == MedicineStatus.ACTIVE)
                .map(medicine -> toResponse(medicine, categoryNames, stock, settings))
                .filter(response -> matches(filter, response))
                .map(response -> salesView ? response.withoutCost() : response)
                .toList();
        return ListPager.paginate(items, pageQuery, SORTABLE, DEFAULT_SORT);
    }

    @Override
    public List<String> manufacturers(String q, int limit) {
        return medicineRepository.findAll().stream()
                .map(Medicine::getManufacturer)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .filter(name -> TextSearch.matchesAny(q, name))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(limit)
                .toList();
    }

    @Override
    public MedicineResponse get(String id, Actor actor) {
        Medicine medicine = medicineRepository.findById(id).orElseThrow(NotFoundException::new);
        SystemSetting settings = settingService.current();
        boolean salesView = actor.role() == Role.SALES;
        MedicineResponse base = toResponse(
                medicine,
                categoryNames(),
                Map.of(medicine.getId(), stockQueryService.summarize(medicine.getId())),
                settings);
        List<BatchResponse> batches = batchesOf(medicine, salesView);
        List<MovementResponse> movements = salesView
                ? List.of()
                : movementRepository.findTop30ByMedicineIdOrderByCreatedAtDesc(medicine.getId()).stream()
                        .map(movementMapper::toResponse)
                        .toList();
        MedicineResponse detailed = base.withDetails(batches, movements);
        return salesView ? detailed.withoutCost() : detailed;
    }

    private MedicineResponse toResponse(
            Medicine medicine,
            Map<String, String> categoryNames,
            Map<String, StockSummary> stock,
            SystemSetting settings) {
        StockSummary summary = stock.getOrDefault(medicine.getId(), StockSummary.EMPTY);
        return medicineMapper.toResponse(
                medicine,
                categoryNames.getOrDefault(medicine.getCategoryId(), ""),
                summary,
                stockQueryService.statusOf(medicine, summary, settings));
    }

    private boolean matches(MedicineFilter filter, MedicineResponse response) {
        return TextSearch.matchesAny(
                        filter.q(),
                        response.name(),
                        response.code(),
                        response.barcode(),
                        response.genericName(),
                        response.manufacturer())
                && (filter.categoryId() == null || filter.categoryId().equals(response.categoryId()))
                && (filter.status() == null || filter.status() == response.status())
                && (filter.stockStatus() == null || filter.stockStatus() == response.stockStatus())
                && (filter.prescriptionType() == null || filter.prescriptionType() == response.prescriptionType())
                && (!filter.inStock() || response.availableQty() > 0);
    }

    private Map<String, String> categoryNames() {
        return categoryRepository.findAll().stream().collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private List<BatchResponse> batchesOf(Medicine medicine, boolean salesView) {
        List<StockBatch> batches = batchRepository.findByMedicineId(medicine.getId()).stream()
                .filter(batch -> !salesView || batch.getRemainingQty() > 0)
                .sorted(Comparator.comparing(StockBatch::getExpiryDate))
                .toList();
        return batchQueryService.toResponses(batches, salesView);
    }

    @Override
    public List<MedicineResponse> lowStock(int limit) {
        SystemSetting settings = settingService.current();
        Map<String, StockSummary> stock = stockQueryService.summarizeAll();
        Map<String, String> categoryNames = categoryNames();
        return medicineRepository.findAll().stream()
                .filter(medicine -> medicine.getStatus() == MedicineStatus.ACTIVE)
                .map(medicine -> toResponse(medicine, categoryNames, stock, settings))
                .filter(response -> response.stockStatus() != StockStatus.IN_STOCK)
                .sorted(Comparator.comparingDouble(
                        response -> (double) response.availableQty() / Math.max(1, response.minStock())))
                .limit(limit)
                .toList();
    }
}
