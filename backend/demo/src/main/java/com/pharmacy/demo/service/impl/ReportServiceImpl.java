package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.response.BestSellerResponse;
import com.pharmacy.demo.dto.response.CategoryDistributionResponse;
import com.pharmacy.demo.dto.response.EmployeeRevenueResponse;
import com.pharmacy.demo.dto.response.ReportSummaryResponse;
import com.pharmacy.demo.dto.response.RevenuePointResponse;
import com.pharmacy.demo.entity.Category;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.embedded.InvoiceLine;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.exception.BadRequestException;
import com.pharmacy.demo.repository.CategoryRepository;
import com.pharmacy.demo.repository.MedicineRepository;
import com.pharmacy.demo.service.ReportService;
import com.pharmacy.demo.service.StockQueryService;
import com.pharmacy.demo.service.StockSummary;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final int DEFAULT_RANGE_DAYS = 29;
    private static final int MAX_RANGE_DAYS = 366;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private final MongoTemplate mongoTemplate;
    private final CategoryRepository categoryRepository;
    private final MedicineRepository medicineRepository;
    private final StockQueryService stockQueryService;
    private final Clock clock;

    @Override
    public ReportSummaryResponse summary(LocalDate from, LocalDate to) {
        DateRange range = rangeOf(from, to);
        List<Invoice> invoices = invoicesIn(range, null);
        List<Invoice> completed = invoices.stream()
                .filter(invoice -> invoice.getStatus() == InvoiceStatus.COMPLETED)
                .toList();
        long revenue = completed.stream().mapToLong(Invoice::getTotal).sum();
        long itemsSold = completed.stream()
                .flatMap(invoice -> invoice.getItems().stream())
                .mapToLong(InvoiceLine::getQuantity)
                .sum();
        return new ReportSummaryResponse(
                range.from(),
                range.to(),
                revenue,
                completed.size(),
                invoices.size() - completed.size(),
                completed.isEmpty() ? 0 : Math.round((double) revenue / completed.size()),
                itemsSold);
    }

    @Override
    public List<RevenuePointResponse> revenue(LocalDate from, LocalDate to) {
        DateRange range = rangeOf(from, to);
        Map<LocalDate, long[]> buckets = new LinkedHashMap<>();
        for (LocalDate day = range.from(); !day.isAfter(range.to()); day = day.plusDays(1)) {
            buckets.put(day, new long[2]);
        }
        for (Invoice invoice : invoicesIn(range, InvoiceStatus.COMPLETED)) {
            long[] bucket =
                    buckets.get(invoice.getCreatedAt().atZone(clock.getZone()).toLocalDate());
            if (bucket != null) {
                bucket[0] += invoice.getTotal();
                bucket[1] += 1;
            }
        }
        List<RevenuePointResponse> series = new ArrayList<>();
        buckets.forEach((day, bucket) -> series.add(new RevenuePointResponse(day, bucket[0], bucket[1])));
        return series;
    }

    @Override
    public List<BestSellerResponse> bestSellers(LocalDate from, LocalDate to, int limit) {
        int safeLimit = limit < 1 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        Map<String, long[]> totals = new LinkedHashMap<>();
        Map<String, InvoiceLine> samples = new LinkedHashMap<>();
        for (Invoice invoice : invoicesIn(rangeOf(from, to), InvoiceStatus.COMPLETED)) {
            for (InvoiceLine line : invoice.getItems()) {
                long[] total = totals.computeIfAbsent(line.getMedicineId(), key -> new long[2]);
                total[0] += line.getQuantity();
                total[1] += line.getSubtotal();
                samples.putIfAbsent(line.getMedicineId(), line);
            }
        }
        return totals.entrySet().stream()
                .map(entry -> {
                    InvoiceLine sample = samples.get(entry.getKey());
                    return new BestSellerResponse(
                            entry.getKey(),
                            sample.getMedicineName(),
                            sample.getMedicineCode(),
                            sample.getUnit(),
                            entry.getValue()[0],
                            entry.getValue()[1]);
                })
                .sorted(Comparator.comparingLong(BestSellerResponse::quantity)
                        .thenComparingLong(BestSellerResponse::revenue)
                        .reversed())
                .limit(safeLimit)
                .toList();
    }

    @Override
    public List<CategoryDistributionResponse> inventoryDistribution() {
        Map<String, StockSummary> stock = stockQueryService.summarizeAll();
        Map<String, long[]> totals = new LinkedHashMap<>();
        for (Medicine medicine : medicineRepository.findAll()) {
            long[] total = totals.computeIfAbsent(medicine.getCategoryId(), key -> new long[3]);
            long available =
                    stock.getOrDefault(medicine.getId(), StockSummary.EMPTY).availableQty();
            total[0] += 1;
            total[1] += available;
            total[2] += available * (medicine.getCostPrice() == null ? 0 : medicine.getCostPrice());
        }
        return categoryRepository.findAll(Sort.by("id")).stream()
                .filter(category -> totals.containsKey(category.getId()))
                .map(category -> distributionOf(category, totals.get(category.getId())))
                .toList();
    }

    @Override
    public List<EmployeeRevenueResponse> revenueByEmployee(LocalDate from, LocalDate to) {
        Map<String, long[]> totals = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (Invoice invoice : invoicesIn(rangeOf(from, to), InvoiceStatus.COMPLETED)) {
            long[] total = totals.computeIfAbsent(invoice.getEmployeeId(), key -> new long[2]);
            total[0] += 1;
            total[1] += invoice.getTotal();
            names.putIfAbsent(invoice.getEmployeeId(), invoice.getEmployeeName());
        }
        return totals.entrySet().stream()
                .map(entry -> new EmployeeRevenueResponse(
                        entry.getKey(), names.get(entry.getKey()), entry.getValue()[0], entry.getValue()[1]))
                .sorted(Comparator.comparingLong(EmployeeRevenueResponse::revenue)
                        .reversed())
                .toList();
    }

    private static CategoryDistributionResponse distributionOf(Category category, long[] total) {
        return new CategoryDistributionResponse(
                category.getId(), category.getName(), category.getColor(), total[0], total[1], total[2]);
    }

    private List<Invoice> invoicesIn(DateRange range, InvoiceStatus status) {
        Criteria createdInRange = Criteria.where("createdAt")
                .gte(range.from().atStartOfDay(clock.getZone()).toInstant())
                .lt(range.to().plusDays(1).atStartOfDay(clock.getZone()).toInstant());
        Criteria criteria = status == null
                ? createdInRange
                : new Criteria()
                        .andOperator(createdInRange, Criteria.where("status").is(status));
        return mongoTemplate.find(Query.query(criteria), Invoice.class);
    }

    private DateRange rangeOf(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate end = to == null ? today : to;
        LocalDate start = from == null ? end.minusDays(DEFAULT_RANGE_DAYS) : from;
        if (start.isAfter(end)) {
            throw new BadRequestException("Khoảng ngày không hợp lệ: ngày bắt đầu phải trước ngày kết thúc.");
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw new BadRequestException("Khoảng ngày tối đa " + MAX_RANGE_DAYS + " ngày.");
        }
        return new DateRange(start, end);
    }

    private record DateRange(LocalDate from, LocalDate to) {}
}
