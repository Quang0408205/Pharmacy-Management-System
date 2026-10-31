package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.response.ActivityResponse;
import com.pharmacy.demo.dto.response.AdminDashboardResponse;
import com.pharmacy.demo.dto.response.InventorySummaryResponse;
import com.pharmacy.demo.dto.response.InvoiceSummaryResponse;
import com.pharmacy.demo.dto.response.ReportSummaryResponse;
import com.pharmacy.demo.dto.response.SalesDashboardResponse;
import com.pharmacy.demo.entity.Invoice;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.mapper.ActivityLogMapper;
import com.pharmacy.demo.mapper.InvoiceMapper;
import com.pharmacy.demo.repository.ActivityLogRepository;
import com.pharmacy.demo.repository.InvoiceDraftRepository;
import com.pharmacy.demo.repository.InvoiceRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.BatchQueryService;
import com.pharmacy.demo.service.DashboardService;
import com.pharmacy.demo.service.InventoryService;
import com.pharmacy.demo.service.MedicineService;
import com.pharmacy.demo.service.ReportService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int SERIES_DAYS = 14;
    private static final int LIST_SIZE = 6;

    private final ReportService reportService;
    private final InventoryService inventoryService;
    private final MedicineService medicineService;
    private final BatchQueryService batchQueryService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDraftRepository invoiceDraftRepository;
    private final MongoTemplate mongoTemplate;
    private final ActivityLogRepository activityLogRepository;
    private final InvoiceMapper invoiceMapper;
    private final ActivityLogMapper activityLogMapper;
    private final Clock clock;

    @Override
    public AdminDashboardResponse admin() {
        LocalDate today = LocalDate.now(clock);
        ReportSummaryResponse todaySummary = reportService.summary(today, today);
        ReportSummaryResponse monthSummary = reportService.summary(today.withDayOfMonth(1), today);
        InventorySummaryResponse inventory = inventoryService.summary();
        PageRequest newestFirst = PageRequest.of(0, LIST_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<InvoiceSummaryResponse> recentInvoices = invoiceRepository.findAll(newestFirst).stream()
                .map(invoiceMapper::toSummary)
                .toList();
        List<ActivityResponse> activities = activityLogRepository.findAll(newestFirst).stream()
                .map(activityLogMapper::toResponse)
                .toList();
        return new AdminDashboardResponse(
                new AdminDashboardResponse.Kpis(
                        todaySummary.revenue(),
                        monthSummary.revenue(),
                        todaySummary.invoices(),
                        monthSummary.invoices(),
                        inventory.activeMedicines(),
                        inventory.lowStock() + inventory.outOfStock(),
                        inventory.expiringBatches()),
                reportService.revenue(today.minusDays(SERIES_DAYS - 1), today),
                reportService.inventoryDistribution(),
                recentInvoices,
                medicineService.lowStock(LIST_SIZE),
                batchQueryService.expiringSoon(LIST_SIZE),
                activities);
    }

    @Override
    public SalesDashboardResponse sales(Actor actor) {
        Instant startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        List<Invoice> invoicesToday = mongoTemplate.find(
                Query.query(Criteria.where("employeeId")
                        .is(actor.id())
                        .and("status")
                        .is(InvoiceStatus.COMPLETED)
                        .and("createdAt")
                        .gte(startOfDay)),
                Invoice.class);
        return new SalesDashboardResponse(
                invoicesToday.size(),
                invoicesToday.stream().mapToLong(Invoice::getTotal).sum(),
                invoiceDraftRepository.countByEmployeeId(actor.id()));
    }
}
