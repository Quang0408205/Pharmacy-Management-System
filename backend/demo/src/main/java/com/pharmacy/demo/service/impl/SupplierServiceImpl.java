package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.common.ListPager;
import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.SupplierRequest;
import com.pharmacy.demo.dto.response.ReceiptSummaryResponse;
import com.pharmacy.demo.dto.response.SupplierDetailResponse;
import com.pharmacy.demo.dto.response.SupplierResponse;
import com.pharmacy.demo.entity.GoodsReceipt;
import com.pharmacy.demo.entity.Supplier;
import com.pharmacy.demo.enums.ReceiptStatus;
import com.pharmacy.demo.enums.SupplierStatus;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.ReceiptMapper;
import com.pharmacy.demo.mapper.SupplierMapper;
import com.pharmacy.demo.repository.GoodsReceiptRepository;
import com.pharmacy.demo.repository.SupplierRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.CodeGenerator;
import com.pharmacy.demo.service.CodeKind;
import com.pharmacy.demo.service.IdGenerator;
import com.pharmacy.demo.service.SupplierService;
import com.pharmacy.demo.util.PhoneMatcher;
import com.pharmacy.demo.util.TextSearch;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private static final String DEFAULT_PAYMENT_TERMS = "Thanh toán ngay";
    private static final Map<String, Comparator<SupplierResponse>> SORTABLE = Map.of(
            "code", ListPager.text(SupplierResponse::code),
            "name", ListPager.text(SupplierResponse::name),
            "contactName", ListPager.text(SupplierResponse::contactName),
            "status", ListPager.comparable(SupplierResponse::status),
            "receiptCount", ListPager.comparable(SupplierResponse::receiptCount));

    private final SupplierRepository supplierRepository;
    private final GoodsReceiptRepository receiptRepository;
    private final SupplierMapper supplierMapper;
    private final ReceiptMapper receiptMapper;
    private final CodeGenerator codeGenerator;
    private final ActivityLogger activityLogger;
    private final IdGenerator idGenerator;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    public PageResponse<SupplierResponse> search(String q, SupplierStatus status, PageQuery query) {
        Map<String, ConfirmedTotals> totals = confirmedTotals();
        List<SupplierResponse> items = supplierRepository.findAll().stream()
                .filter(supplier -> status == null || supplier.getStatus() == status)
                .filter(supplier -> TextSearch.matchesAny(
                        q,
                        supplier.getName(),
                        supplier.getCode(),
                        supplier.getContactName(),
                        supplier.getPhone(),
                        supplier.getTaxCode()))
                .map(supplier -> supplierMapper.toResponse(
                        supplier,
                        totals.getOrDefault(supplier.getId(), ConfirmedTotals.EMPTY)
                                .count()))
                .toList();
        return ListPager.paginate(items, query, SORTABLE, "code,asc");
    }

    @Override
    public SupplierDetailResponse get(String id) {
        Supplier supplier = find(id);
        ConfirmedTotals totals = confirmedTotals().getOrDefault(id, ConfirmedTotals.EMPTY);
        List<ReceiptSummaryResponse> receipts =
                receiptRepository.findTop20BySupplierIdOrderByReceivingDateDesc(id).stream()
                        .map(receiptMapper::toSummary)
                        .toList();
        return supplierMapper.toDetail(supplier, receiptRepository.countBySupplierId(id), totals.amount(), receipts);
    }

    @Override
    @Transactional
    public SupplierResponse create(SupplierRequest request, Actor actor) {
        requireUniquePhone(request.phone(), null);
        Instant now = clock.instant();
        Supplier supplier = Supplier.builder()
                .id(idGenerator.newId())
                .code(codeGenerator.next(CodeKind.SUPPLIER))
                .status(SupplierStatus.ACTIVE)
                .createdAt(now)
                .build();
        apply(supplier, request, now);
        supplierRepository.save(supplier);
        activityLogger.log(
                actor,
                "CREATE_SUPPLIER",
                "SUPPLIER",
                supplier.getId(),
                supplier.getCode(),
                "Thêm nhà cung cấp " + supplier.getName());
        return supplierMapper.toResponse(supplier, 0);
    }

    @Override
    @Transactional
    public SupplierResponse update(String id, SupplierRequest request, Actor actor) {
        Supplier supplier = find(id);
        requireUniquePhone(request.phone(), supplier.getId());
        apply(supplier, request, clock.instant());
        supplierRepository.save(supplier);
        activityLogger.log(
                actor,
                "UPDATE_SUPPLIER",
                "SUPPLIER",
                supplier.getId(),
                supplier.getCode(),
                "Cập nhật nhà cung cấp " + supplier.getName());
        return respond(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse changeStatus(String id, SupplierStatus status, Actor actor) {
        Supplier supplier = find(id);
        if (supplier.getStatus() != status) {
            supplier.setStatus(status);
            supplier.setUpdatedAt(clock.instant());
            supplierRepository.save(supplier);
            activityLogger.log(
                    actor,
                    "UPDATE_SUPPLIER_STATUS",
                    "SUPPLIER",
                    supplier.getId(),
                    supplier.getCode(),
                    (status == SupplierStatus.ACTIVE ? "Kích hoạt" : "Ngừng hợp tác") + " nhà cung cấp "
                            + supplier.getName());
        }
        return respond(supplier);
    }

    private Supplier find(String id) {
        return supplierRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy nhà cung cấp."));
    }

    private SupplierResponse respond(Supplier supplier) {
        return supplierMapper.toResponse(
                supplier,
                confirmedTotals()
                        .getOrDefault(supplier.getId(), ConfirmedTotals.EMPTY)
                        .count());
    }

    private void apply(Supplier supplier, SupplierRequest request, Instant now) {
        supplier.setName(request.name().trim());
        supplier.setContactName(text(request.contactName()));
        supplier.setPhone(request.phone().trim());
        supplier.setEmail(text(request.email()));
        supplier.setAddress(text(request.address()));
        supplier.setTaxCode(text(request.taxCode()));
        supplier.setPaymentTerms(
                request.paymentTerms() == null || request.paymentTerms().isBlank()
                        ? DEFAULT_PAYMENT_TERMS
                        : request.paymentTerms().trim());
        supplier.setNote(text(request.note()));
        supplier.setUpdatedAt(now);
    }

    private void requireUniquePhone(String phone, String selfId) {
        Criteria criteria = Criteria.where("phone").regex(PhoneMatcher.sameNumberRegex(phone));
        if (selfId != null) {
            criteria = criteria.and("id").ne(selfId);
        }
        if (mongoTemplate.exists(Query.query(criteria), Supplier.class)) {
            throw new ConflictException(
                    "Số điện thoại đã tồn tại.", Map.of("phone", "Số điện thoại đã được dùng cho nhà cung cấp khác."));
        }
    }

    private Map<String, ConfirmedTotals> confirmedTotals() {
        TypedAggregation<GoodsReceipt> aggregation = Aggregation.newAggregation(
                GoodsReceipt.class,
                Aggregation.match(Criteria.where("status").is(ReceiptStatus.CONFIRMED)),
                Aggregation.group("supplierId")
                        .count()
                        .as("count")
                        .sum("totalAmount")
                        .as("amount"));
        Map<String, ConfirmedTotals> totals = new HashMap<>();
        for (Document row : mongoTemplate.aggregate(aggregation, Document.class).getMappedResults()) {
            totals.put(
                    row.getString("_id"),
                    new ConfirmedTotals(
                            row.get("count", Number.class).longValue(),
                            row.get("amount", Number.class).longValue()));
        }
        return totals;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private record ConfirmedTotals(long count, long amount) {

        static final ConfirmedTotals EMPTY = new ConfirmedTotals(0, 0);
    }
}
