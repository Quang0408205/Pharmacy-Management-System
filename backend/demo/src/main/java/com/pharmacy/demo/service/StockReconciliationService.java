package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.response.ReconciliationResponse;
import java.util.List;

public interface StockReconciliationService {

    List<BatchMismatch> findMismatches();

    ReconciliationResponse report();

    record BatchMismatch(String batchId, String batchCode, int remainingQty, int ledgerTotal) {}
}
