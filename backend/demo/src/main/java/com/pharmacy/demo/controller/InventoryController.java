package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.response.InventorySummaryResponse;
import com.pharmacy.demo.dto.response.ReconciliationResponse;
import com.pharmacy.demo.service.InventoryService;
import com.pharmacy.demo.service.StockReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tồn kho", description = "Tổng hợp tồn kho và đối soát sổ kho.")
@RestController
@RequestMapping("/api/v1/inventory")
@PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final StockReconciliationService reconciliationService;

    @Operation(summary = "Đối soát tồn từng lô với tổng biến động, trả các lô lệch (Admin)")
    @GetMapping("/reconciliation")
    @PreAuthorize("hasRole('ADMIN')")
    public ReconciliationResponse reconciliation() {
        return reconciliationService.report();
    }

    @Operation(summary = "Tổng hợp tồn kho")
    @GetMapping("/summary")
    public InventorySummaryResponse summary() {
        return inventoryService.summary();
    }
}
