package com.pharmacy.demo.dto.response;

import java.time.LocalDate;
import java.util.List;

public record WarehouseDashboardResponse(
        Kpis kpis,
        List<Task> tasks,
        List<MovementPoint> movementSeries,
        List<MedicineResponse> lowStock,
        List<BatchResponse> expiring,
        List<ReceiptSummaryResponse> recentReceipts,
        List<MovementResponse> recentMovements) {

    public record Kpis(
            long totalMedicines,
            long totalBatches,
            long lowStockCount,
            long expiringBatches,
            long pendingReceipts,
            long movementsToday) {}

    public record Task(String id, String label, long count, String tone, String link) {}

    public record MovementPoint(LocalDate date, long inQty, long outQty) {}
}
