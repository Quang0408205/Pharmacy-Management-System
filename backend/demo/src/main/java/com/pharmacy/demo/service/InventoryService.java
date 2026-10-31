package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.response.ExpirySummaryResponse;
import com.pharmacy.demo.dto.response.InventorySummaryResponse;

public interface InventoryService {

    InventorySummaryResponse summary();

    ExpirySummaryResponse expirySummary();
}
