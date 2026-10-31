package com.pharmacy.demo.dto.response;

import java.util.List;

public record AdminDashboardResponse(
        Kpis kpis,
        List<RevenuePointResponse> revenueSeries,
        List<CategoryDistributionResponse> categoryDistribution,
        List<InvoiceSummaryResponse> recentInvoices,
        List<MedicineResponse> lowStock,
        List<BatchResponse> expiring,
        List<ActivityResponse> activities) {

    public record Kpis(
            long todayRevenue,
            long monthRevenue,
            long todayInvoices,
            long monthInvoices,
            long activeMedicines,
            long lowStockCount,
            long expiringBatches) {}
}
