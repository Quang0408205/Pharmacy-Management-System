package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.common.ListPager;
import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.InvoiceDetailResponse;
import com.pharmacy.demo.dto.response.InvoiceSummaryResponse;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.embedded.BatchAllocation;
import com.pharmacy.demo.entity.embedded.InvoiceLine;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.ReferenceType;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.InvoiceMapper;
import com.pharmacy.demo.mapper.SettingMapper;
import com.pharmacy.demo.repository.CustomerRepository;
import com.pharmacy.demo.repository.EmployeeRepository;
import com.pharmacy.demo.repository.InvoiceRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.InvoiceService;
import com.pharmacy.demo.service.LoyaltyService;
import com.pharmacy.demo.service.StockChange;
import com.pharmacy.demo.service.StockLedgerService;
import com.pharmacy.demo.service.SystemSettingService;
import com.pharmacy.demo.util.TextSearch;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private static final String NOT_FOUND_MESSAGE = "Không tìm thấy hóa đơn.";
    private static final Map<String, Comparator<InvoiceSummaryResponse>> SORTABLE = Map.of(
            "code", ListPager.text(InvoiceSummaryResponse::code),
            "createdAt", ListPager.comparable(InvoiceSummaryResponse::createdAt),
            "customerName", ListPager.text(InvoiceSummaryResponse::customerName),
            "employeeName", ListPager.text(InvoiceSummaryResponse::employeeName),
            "total", ListPager.comparable(InvoiceSummaryResponse::total),
            "status", ListPager.comparable(InvoiceSummaryResponse::status),
            "paymentMethod", ListPager.comparable(InvoiceSummaryResponse::paymentMethod));

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final StockLedgerService stockLedgerService;
    private final LoyaltyService loyaltyService;
    private final SystemSettingService settingService;
    private final InvoiceMapper invoiceMapper;
    private final SettingMapper settingMapper;
    private final ActivityLogger activityLogger;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    public PageResponse<InvoiceSummaryResponse> search(InvoiceFilter filter, PageQuery query, Actor actor) {
        List<InvoiceSummaryResponse> items =
                mongoTemplate.find(Query.query(criteriaOf(filter, actor)), Invoice.class).stream()
                        .filter(invoice -> TextSearch.matchesAny(
                                filter.q(), invoice.getCode(), invoice.getCustomerName(), invoice.getEmployeeName()))
                        .map(invoiceMapper::toSummary)
                        .toList();
        return ListPager.paginate(items, query, SORTABLE, "createdAt,desc");
    }

    @Override
    public InvoiceDetailResponse get(String id, Actor actor) {
        Invoice invoice = invoiceRepository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        if (actor.role() == Role.SALES && !actor.id().equals(invoice.getEmployeeId())) {
            throw new NotFoundException(NOT_FOUND_MESSAGE);
        }
        return detailOf(invoice);
    }

    @Override
    @Transactional
    public InvoiceDetailResponse cancel(String id, String reason, Actor actor) {
        invoiceRepository.findById(id).orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        String cleanReason = reason.trim();
        Invoice invoice = mongoTemplate.findAndModify(
                Query.query(Criteria.where("_id").is(id).and("status").is(InvoiceStatus.COMPLETED)),
                new Update()
                        .set("status", InvoiceStatus.CANCELLED)
                        .set("cancelReason", cleanReason)
                        .set("cancelledById", actor.id())
                        .set("cancelledByName", actor.name())
                        .set("cancelledAt", clock.instant()),
                FindAndModifyOptions.options().returnNew(true),
                Invoice.class);
        if (invoice == null) {
            throw new ConflictException("Hóa đơn đã được hủy trước đó.");
        }
        for (InvoiceLine line : invoice.getItems()) {
            for (BatchAllocation allocation : line.getAllocations()) {
                StockBatch restored = stockLedgerService.apply(new StockChange(
                        allocation.getBatchId(),
                        allocation.getQuantity(),
                        MovementType.RETURN,
                        ReferenceType.INVOICE_CANCEL,
                        invoice.getId(),
                        invoice.getCode(),
                        actor,
                        "Hoàn kho do hủy hóa đơn: " + cleanReason,
                        null,
                        null));
                if (restored.getHandlingStatus() == BatchHandlingStatus.DISPOSED) {
                    stockLedgerService.changeHandlingStatus(
                            restored.getId(), BatchHandlingStatus.QUARANTINED, Set.of(BatchHandlingStatus.DISPOSED));
                }
            }
        }
        loyaltyService.reverse(invoice, actor);
        activityLogger.log(
                actor,
                "CANCEL_INVOICE",
                "INVOICE",
                invoice.getId(),
                invoice.getCode(),
                "Hủy hóa đơn " + invoice.getCode() + ": " + cleanReason);
        return detailOf(invoice);
    }

    private Criteria criteriaOf(InvoiceFilter filter, Actor actor) {
        Criteria criteria = new Criteria();
        String employeeId = filter.employeeId();
        if (actor.role() == Role.SALES || filter.mine()) {
            employeeId = actor.id();
        }
        if (employeeId != null) {
            criteria = criteria.and("employeeId").is(employeeId);
        }
        if (filter.status() != null) {
            criteria = criteria.and("status").is(filter.status());
        }
        if (filter.paymentMethod() != null) {
            criteria = criteria.and("paymentMethod").is(filter.paymentMethod());
        }
        if (filter.from() != null || filter.to() != null) {
            Criteria range = Criteria.where("createdAt");
            if (filter.from() != null) {
                range = range.gte(filter.from().atStartOfDay(clock.getZone()).toInstant());
            }
            if (filter.to() != null) {
                range = range.lt(
                        filter.to().plusDays(1).atStartOfDay(clock.getZone()).toInstant());
            }
            criteria = new Criteria().andOperator(criteria, range);
        }
        return criteria;
    }

    private InvoiceDetailResponse detailOf(Invoice invoice) {
        InvoiceDetailResponse.CustomerRef customer = invoice.getCustomerId() == null
                ? null
                : customerRepository
                        .findById(invoice.getCustomerId())
                        .map(found -> new InvoiceDetailResponse.CustomerRef(
                                found.getId(), found.getCode(), found.getName(), found.getPhone()))
                        .orElse(null);
        Employee seller = employeeRepository.findById(invoice.getEmployeeId()).orElse(null);
        InvoiceDetailResponse.EmployeeRef employee = seller == null
                ? null
                : new InvoiceDetailResponse.EmployeeRef(
                        seller.getId(), seller.getFullName(), seller.getRole(), seller.getEmail());
        return InvoiceDetailResponse.withParties(
                invoiceMapper.toDetail(invoice),
                customer,
                employee,
                settingMapper.toPharmacy(settingService.current().getPharmacy()));
    }
}
