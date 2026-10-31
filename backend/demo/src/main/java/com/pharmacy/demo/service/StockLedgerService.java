package com.pharmacy.demo.service;

import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.ReferenceType;
import com.pharmacy.demo.security.Actor;
import java.util.Set;

public interface StockLedgerService {

    StockBatch receiveBatch(
            StockBatch batch,
            ReferenceType referenceType,
            String referenceId,
            String referenceCode,
            Actor actor,
            String note);

    StockBatch apply(StockChange change);

    StockBatch changeHandlingStatus(String batchId, BatchHandlingStatus target, Set<BatchHandlingStatus> allowedFrom);
}
