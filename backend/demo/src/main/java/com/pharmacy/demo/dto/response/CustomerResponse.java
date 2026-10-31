package com.pharmacy.demo.dto.response;

import java.time.Instant;

public record CustomerResponse(
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
        Instant lastPurchase) {}
