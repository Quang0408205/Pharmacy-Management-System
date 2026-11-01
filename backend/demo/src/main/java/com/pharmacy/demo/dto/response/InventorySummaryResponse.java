package com.pharmacy.demo.dto.response;

public record InventorySummaryResponse(
        long totalMedicines,
        long activeMedicines,
        long inStockMedicines,
        long totalBatches,
        long totalUnits,
        long stockValue,
        long lowStock,
        long outOfStock,
        long expiringBatches,
        long expiredBatches,
        int expiryWarningDays) {}
