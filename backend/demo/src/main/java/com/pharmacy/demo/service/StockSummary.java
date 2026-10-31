package com.pharmacy.demo.service;

import java.time.LocalDate;

public record StockSummary(
        int totalQty, int availableQty, int expiredQty, int batchCount, LocalDate nearestExpiry, int expiringQty) {

    public static final StockSummary EMPTY = new StockSummary(0, 0, 0, 0, null, 0);
}
