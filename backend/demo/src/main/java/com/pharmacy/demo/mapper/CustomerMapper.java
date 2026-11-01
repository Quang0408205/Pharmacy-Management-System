package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.CustomerDetailResponse;
import com.pharmacy.demo.dto.response.CustomerResponse;
import com.pharmacy.demo.dto.response.InvoiceSummaryResponse;
import com.pharmacy.demo.entity.Customer;
import com.pharmacy.demo.service.TierStatus;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CustomerMapper {

    @Mapping(target = "tier", source = "tierStatus.tier")
    @Mapping(target = "nextTier", source = "tierStatus.nextTier")
    @Mapping(target = "amountToNextTier", source = "tierStatus.amountToNextTier")
    @Mapping(target = "earnRatePercent", source = "tierStatus.earnRatePercent")
    CustomerResponse toResponse(
            Customer customer, long invoiceCount, long totalSpent, Instant lastPurchase, TierStatus tierStatus);

    @Mapping(target = "tier", source = "tierStatus.tier")
    @Mapping(target = "nextTier", source = "tierStatus.nextTier")
    @Mapping(target = "amountToNextTier", source = "tierStatus.amountToNextTier")
    @Mapping(target = "earnRatePercent", source = "tierStatus.earnRatePercent")
    CustomerDetailResponse toDetail(
            Customer customer,
            long invoiceCount,
            long totalSpent,
            Instant lastPurchase,
            TierStatus tierStatus,
            List<InvoiceSummaryResponse> invoices);
}
