package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.MembershipTier;
import java.time.Instant;
import java.util.List;

public record CustomerDetailResponse(
        String id,
        String code,
        String name,
        String phone,
        String email,
        String address,
        String gender,
        Integer birthYear,
        String note,
        int loyaltyPoints,
        Instant createdAt,
        long invoiceCount,
        long totalSpent,
        Instant lastPurchase,
        MembershipTier tier,
        MembershipTier nextTier,
        long amountToNextTier,
        int earnRatePercent,
        List<InvoiceSummaryResponse> invoices) {}
