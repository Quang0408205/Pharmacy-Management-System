package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.response.ExpirySummaryResponse;
import com.pharmacy.demo.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Hạn sử dụng", description = "Thống kê lô theo ngưỡng hạn dùng.")
@RestController
@RequestMapping("/api/v1/expiry")
@PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
@RequiredArgsConstructor
public class ExpiryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Số lô theo từng ngưỡng cảnh báo và số lô đã hết hạn")
    @GetMapping("/summary")
    public ExpirySummaryResponse summary() {
        return inventoryService.expirySummary();
    }
}
