package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.common.ListPager;
import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.CustomerRequest;
import com.pharmacy.demo.dto.response.CustomerDetailResponse;
import com.pharmacy.demo.dto.response.CustomerResponse;
import com.pharmacy.demo.dto.response.InvoiceSummaryResponse;
import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.exception.ConflictException;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.CustomerMapper;
import com.pharmacy.demo.mapper.InvoiceMapper;
import com.pharmacy.demo.repository.CustomerRepository;
import com.pharmacy.demo.repository.InvoiceRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ActivityLogger;
import com.pharmacy.demo.service.CodeGenerator;
import com.pharmacy.demo.service.CodeKind;
import com.pharmacy.demo.service.CustomerService;
import com.pharmacy.demo.service.IdGenerator;
import com.pharmacy.demo.util.PhoneMatcher;
import com.pharmacy.demo.util.TextSearch;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
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
public class CustomerServiceImpl implements CustomerService {

    private static final String DEFAULT_GENDER = "Nam";
    private static final Map<String, Comparator<CustomerResponse>> SORTABLE = Map.of(
            "code", ListPager.text(CustomerResponse::code),
            "name", ListPager.text(CustomerResponse::name),
            "phone", ListPager.text(CustomerResponse::phone),
            "invoiceCount", ListPager.comparable(CustomerResponse::invoiceCount),
            "totalSpent", ListPager.comparable(CustomerResponse::totalSpent),
            "loyaltyPoints", ListPager.comparable(CustomerResponse::loyaltyPoints),
            "lastPurchase", ListPager.comparable(CustomerResponse::lastPurchase));

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerMapper customerMapper;
    private final InvoiceMapper invoiceMapper;
    private final CodeGenerator codeGenerator;
    private final ActivityLogger activityLogger;
    private final IdGenerator idGenerator;
    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    public PageResponse<CustomerResponse> search(String q, PageQuery query) {
        Map<String, Purchases> purchases = purchases();
        List<CustomerResponse> items = customerRepository.findAll().stream()
                .filter(customer -> TextSearch.matchesAny(
                        q, customer.getName(), customer.getCode(), customer.getPhone(), customer.getEmail()))
                .map(customer -> toResponse(customer, purchases.getOrDefault(customer.getId(), Purchases.EMPTY)))
                .toList();
        return ListPager.paginate(items, query, SORTABLE, "code,desc");
    }

    @Override
    public CustomerDetailResponse get(String id) {
        Customer customer = find(id);
        Purchases purchases = purchases().getOrDefault(id, Purchases.EMPTY);
        List<InvoiceSummaryResponse> invoices = invoiceRepository.findTop30ByCustomerIdOrderByCreatedAtDesc(id).stream()
                .map(invoiceMapper::toSummary)
                .toList();
        return customerMapper.toDetail(
                customer, purchases.count(), purchases.total(), purchases.lastPurchase(), invoices);
    }

    @Override
    public CustomerResponse findByPhone(String phone) {
        Customer customer = mongoTemplate.findOne(
                Query.query(Criteria.where("phone").regex(PhoneMatcher.sameNumberRegex(phone))), Customer.class);
        if (customer == null) {
            throw new NotFoundException("Không tìm thấy khách hàng với số điện thoại này.");
        }
        return toResponse(customer, purchases().getOrDefault(customer.getId(), Purchases.EMPTY));
    }

    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest request, Actor actor) {
        requireUniquePhone(request.phone(), null);
        Instant now = clock.instant();
        Customer customer = Customer.builder()
                .id(idGenerator.newId())
                .code(codeGenerator.next(CodeKind.CUSTOMER))
                .createdAt(now)
                .build();
        apply(customer, request, now);
        customerRepository.save(customer);
        activityLogger.log(
                actor,
                "CREATE_CUSTOMER",
                "CUSTOMER",
                customer.getId(),
                customer.getCode(),
                "Thêm khách hàng " + customer.getName());
        return toResponse(customer, Purchases.EMPTY);
    }

    @Override
    @Transactional
    public CustomerResponse update(String id, CustomerRequest request, Actor actor) {
        Customer customer = find(id);
        requireUniquePhone(request.phone(), customer.getId());
        apply(customer, request, clock.instant());
        customerRepository.save(customer);
        activityLogger.log(
                actor,
                "UPDATE_CUSTOMER",
                "CUSTOMER",
                customer.getId(),
                customer.getCode(),
                "Cập nhật khách hàng " + customer.getName());
        return toResponse(customer, purchases().getOrDefault(customer.getId(), Purchases.EMPTY));
    }

    private Customer find(String id) {
        return customerRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy khách hàng."));
    }

    private CustomerResponse toResponse(Customer customer, Purchases purchases) {
        return customerMapper.toResponse(customer, purchases.count(), purchases.total(), purchases.lastPurchase());
    }

    private void apply(Customer customer, CustomerRequest request, Instant now) {
        customer.setName(request.name().trim());
        customer.setPhone(request.phone().trim());
        customer.setEmail(text(request.email()));
        customer.setAddress(text(request.address()));
        customer.setGender(
                request.gender() == null || request.gender().isBlank()
                        ? DEFAULT_GENDER
                        : request.gender().trim());
        customer.setBirthYear(request.birthYear());
        customer.setNote(text(request.note()));
        customer.setUpdatedAt(now);
    }

    private void requireUniquePhone(String phone, String selfId) {
        Criteria criteria = Criteria.where("phone").regex(PhoneMatcher.sameNumberRegex(phone));
        if (selfId != null) {
            criteria = criteria.and("id").ne(selfId);
        }
        if (mongoTemplate.exists(Query.query(criteria), Customer.class)) {
            throw new ConflictException(
                    "Số điện thoại đã tồn tại.", Map.of("phone", "Số điện thoại đã được dùng cho khách hàng khác."));
        }
    }

    private Map<String, Purchases> purchases() {
        TypedAggregation<Invoice> aggregation = Aggregation.newAggregation(
                Invoice.class,
                Aggregation.match(Criteria.where("status")
                        .is(InvoiceStatus.COMPLETED)
                        .and("customerId")
                        .ne(null)),
                Aggregation.group("customerId")
                        .count()
                        .as("count")
                        .sum("total")
                        .as("total")
                        .max("createdAt")
                        .as("lastPurchase"));
        Map<String, Purchases> result = new HashMap<>();
        for (Document row : mongoTemplate.aggregate(aggregation, Document.class).getMappedResults()) {
            Date last = row.getDate("lastPurchase");
            result.put(
                    row.getString("_id"),
                    new Purchases(
                            row.get("count", Number.class).longValue(),
                            row.get("total", Number.class).longValue(),
                            last == null ? null : last.toInstant()));
        }
        return result;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private record Purchases(long count, long total, Instant lastPurchase) {

        static final Purchases EMPTY = new Purchases(0, 0, null);
    }
}
