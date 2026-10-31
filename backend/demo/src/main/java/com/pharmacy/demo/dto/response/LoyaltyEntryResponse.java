package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.LoyaltyType;
import java.time.Instant;

public record LoyaltyEntryResponse(
        String id,
        LoyaltyType type,
        Integer points,
        Integer balanceAfter,
        String invoiceId,
        String invoiceCode,
        String note,
        String actorName,
        Instant createdAt) {}
