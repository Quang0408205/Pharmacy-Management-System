package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.response.ReconciliationResponse;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.StockMovement;
import com.pharmacy.demo.service.StockReconciliationService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockReconciliationServiceImpl implements StockReconciliationService {

    private final MongoTemplate mongoTemplate;

    @Override
    public ReconciliationResponse report() {
        List<ReconciliationResponse.Mismatch> mismatches = findMismatches().stream()
                .map(item -> new ReconciliationResponse.Mismatch(
                        item.batchId(), item.batchCode(), item.remainingQty(), item.ledgerTotal()))
                .toList();
        return new ReconciliationResponse(
                mongoTemplate.count(new Query(), StockBatch.class), mismatches.isEmpty(), mismatches);
    }

    @Override
    public List<BatchMismatch> findMismatches() {
        Map<String, Integer> ledgerTotals = new HashMap<>();
        mongoTemplate
                .aggregate(
                        Aggregation.newAggregation(
                                Aggregation.group("batchId").sum("quantity").as("total")),
                        StockMovement.class,
                        Document.class)
                .getMappedResults()
                .forEach(row -> ledgerTotals.put(row.getString("_id"), ((Number) row.get("total")).intValue()));
        return mongoTemplate.findAll(StockBatch.class).stream()
                .filter(batch -> ledgerTotals.getOrDefault(batch.getId(), 0) != batch.getRemainingQty())
                .map(batch -> new BatchMismatch(
                        batch.getId(),
                        batch.getCode(),
                        batch.getRemainingQty(),
                        ledgerTotals.getOrDefault(batch.getId(), 0)))
                .toList();
    }
}
