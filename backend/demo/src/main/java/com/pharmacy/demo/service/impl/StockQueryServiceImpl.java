package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.SystemSetting;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.BatchStatus;
import com.pharmacy.demo.enums.StockStatus;
import com.pharmacy.demo.service.BatchStatusCalculator;
import com.pharmacy.demo.service.StockQueryService;
import com.pharmacy.demo.service.StockSummary;
import com.pharmacy.demo.service.SystemSettingService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockQueryServiceImpl implements StockQueryService {

    private final MongoTemplate mongoTemplate;
    private final SystemSettingService settingService;
    private final BatchStatusCalculator batchStatusCalculator;
    private final Clock clock;

    @Override
    public Map<String, StockSummary> summarizeAll() {
        List<StockBatch> batches =
                mongoTemplate.find(Query.query(Criteria.where("remainingQty").gt(0)), StockBatch.class);
        Map<String, List<StockBatch>> byMedicine = new HashMap<>();
        batches.forEach(batch -> byMedicine
                .computeIfAbsent(batch.getMedicineId(), key -> new ArrayList<>())
                .add(batch));
        Map<String, StockSummary> result = new HashMap<>();
        byMedicine.forEach((medicineId, list) -> result.put(medicineId, fold(list)));
        return result;
    }

    @Override
    public StockSummary summarize(String medicineId) {
        List<StockBatch> batches = mongoTemplate.find(
                Query.query(Criteria.where("medicineId")
                        .is(medicineId)
                        .and("remainingQty")
                        .gt(0)),
                StockBatch.class);
        return batches.isEmpty() ? StockSummary.EMPTY : fold(batches);
    }

    @Override
    public StockStatus statusOf(Medicine medicine, StockSummary stock, SystemSetting settings) {
        if (stock.availableQty() <= 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        double threshold = medicine.getMinStock() * settings.getLowStockMultiplier();
        return stock.availableQty() <= threshold ? StockStatus.LOW_STOCK : StockStatus.IN_STOCK;
    }

    @Override
    public BatchStatus batchStatusOf(StockBatch batch, SystemSetting settings) {
        return batchStatusCalculator.statusOf(batch, settings);
    }

    private StockSummary fold(List<StockBatch> batches) {
        LocalDate today = LocalDate.now(clock);
        LocalDate warningLimit = today.plusDays(settingService.current().getExpiryWarningDays());
        int total = 0;
        int available = 0;
        int expired = 0;
        int expiring = 0;
        LocalDate nearest = null;
        for (StockBatch batch : batches) {
            total += batch.getRemainingQty();
            if (batch.getExpiryDate().isBefore(today)) {
                expired += batch.getRemainingQty();
                continue;
            }
            if (batch.getHandlingStatus() != BatchHandlingStatus.AVAILABLE) {
                continue;
            }
            available += batch.getRemainingQty();
            if (nearest == null || batch.getExpiryDate().isBefore(nearest)) {
                nearest = batch.getExpiryDate();
            }
            if (!batch.getExpiryDate().isAfter(warningLimit)) {
                expiring += batch.getRemainingQty();
            }
        }
        return new StockSummary(total, available, expired, batches.size(), nearest, expiring);
    }
}
