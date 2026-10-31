package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.response.AdminDashboardResponse;
import com.pharmacy.demo.dto.response.SalesDashboardResponse;
import com.pharmacy.demo.dto.response.WarehouseDashboardResponse;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.DashboardService;
import com.pharmacy.demo.service.WarehouseDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tổng quan", description = "Số liệu tổng quan theo vai trò.")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final WarehouseDashboardService warehouseDashboardService;

    @Operation(summary = "Tổng quan cho Admin")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboardResponse admin() {
        return dashboardService.admin();
    }

    @Operation(summary = "Tổng quan kho (Admin, Kho)")
    @GetMapping("/warehouse")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public WarehouseDashboardResponse warehouse() {
        return warehouseDashboardService.warehouse();
    }

    @Operation(summary = "Tổng quan bán hàng của người đang đăng nhập")
    @GetMapping("/sales")
    @PreAuthorize("hasRole('SALES')")
    public SalesDashboardResponse sales(@AuthenticationPrincipal Actor actor) {
        return dashboardService.sales(actor);
    }
}
