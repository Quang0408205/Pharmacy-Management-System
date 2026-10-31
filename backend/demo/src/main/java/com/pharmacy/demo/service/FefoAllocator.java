package com.pharmacy.demo.service;

import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.embedded.BatchAllocation;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.exception.ConflictException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FefoAllocator {

    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    public List<BatchAllocation> allocate(Medicine medicine, int quantity) {
        Query query = Query.query(Criteria.where("medicineId")
                        .is(medicine.getId())
                        .and("handlingStatus")
                        .is(BatchHandlingStatus.AVAILABLE)
                        .and("remainingQty")
                        .gt(0)
                        .and("expiryDate")
                        .gte(LocalDate.now(clock)))
                .with(Sort.by("expiryDate", "receivedDate", "id"));
        List<BatchAllocation> allocations = new ArrayList<>();
        int missing = quantity;
        for (StockBatch batch : mongoTemplate.find(query, StockBatch.class)) {
            if (missing == 0) {
                break;
            }
            int taken = Math.min(batch.getRemainingQty(), missing);
            allocations.add(BatchAllocation.builder()
                    .batchId(batch.getId())
                    .batchCode(batch.getCode())
                    .expiryDate(batch.getExpiryDate())
                    .quantity(taken)
                    .build());
            missing -= taken;
        }
        if (missing > 0) {
            throw new ConflictException("Thuốc " + medicine.getName() + " chỉ còn " + (quantity - missing) + " "
                    + medicine.getUnit() + " có thể bán, không đủ " + quantity + ".");
        }
        return allocations;
    }
}
