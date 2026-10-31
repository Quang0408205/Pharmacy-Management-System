package com.pharmacy.demo.service;

import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.entity.SystemSetting;
import com.pharmacy.demo.enums.BatchStatus;
import com.pharmacy.demo.enums.StockStatus;
import java.util.Map;

public interface StockQueryService {

    Map<String, StockSummary> summarizeAll();

    StockSummary summarize(String medicineId);

    StockStatus statusOf(Medicine medicine, StockSummary stock, SystemSetting settings);

    BatchStatus batchStatusOf(StockBatch batch, SystemSetting settings);
}
