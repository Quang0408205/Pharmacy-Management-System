package com.pharmacy.demo.dto.response;

import java.util.List;

public record ReconciliationResponse(long checkedBatches, boolean balanced, List<Mismatch> mismatches) {

    public record Mismatch(String batchId, String batchCode, int remainingQty, int ledgerTotal) {}
}
