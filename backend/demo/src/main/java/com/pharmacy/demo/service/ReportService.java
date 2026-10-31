package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.response.BestSellerResponse;
import com.pharmacy.demo.dto.response.CategoryDistributionResponse;
import com.pharmacy.demo.dto.response.EmployeeRevenueResponse;
import com.pharmacy.demo.dto.response.ReportSummaryResponse;
import com.pharmacy.demo.dto.response.RevenuePointResponse;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    ReportSummaryResponse summary(LocalDate from, LocalDate to);

    List<RevenuePointResponse> revenue(LocalDate from, LocalDate to);

    List<BestSellerResponse> bestSellers(LocalDate from, LocalDate to, int limit);

    List<CategoryDistributionResponse> inventoryDistribution();

    List<EmployeeRevenueResponse> revenueByEmployee(LocalDate from, LocalDate to);
}
