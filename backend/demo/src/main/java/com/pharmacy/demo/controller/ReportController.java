package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.response.BestSellerResponse;
import com.pharmacy.demo.dto.response.CategoryDistributionResponse;
import com.pharmacy.demo.dto.response.EmployeeRevenueResponse;
import com.pharmacy.demo.dto.response.ReportSummaryResponse;
import com.pharmacy.demo.dto.response.RevenuePointResponse;
import com.pharmacy.demo.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Báo cáo", description = "Thống kê doanh thu và tồn kho (Admin).")
@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Tóm tắt doanh thu trong khoảng ngày")
    @GetMapping("/summary")
    public ReportSummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.summary(from, to);
    }

    @Operation(summary = "Doanh thu theo ngày")
    @GetMapping("/revenue")
    public List<RevenuePointResponse> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.revenue(from, to);
    }

    @Operation(summary = "Thuốc bán chạy")
    @GetMapping("/best-sellers")
    public List<BestSellerResponse> bestSellers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return reportService.bestSellers(from, to, limit);
    }

    @Operation(summary = "Phân bố tồn kho theo danh mục")
    @GetMapping("/inventory-distribution")
    public List<CategoryDistributionResponse> inventoryDistribution() {
        return reportService.inventoryDistribution();
    }

    @Operation(summary = "Doanh thu theo nhân viên")
    @GetMapping("/revenue-by-employee")
    public List<EmployeeRevenueResponse> revenueByEmployee(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.revenueByEmployee(from, to);
    }
}
