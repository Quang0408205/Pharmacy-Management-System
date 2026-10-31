package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.DiscountType;
import com.pharmacy.demo.enums.PaymentMethod;
import java.time.Instant;
import java.util.List;

public record InvoiceDraftResponse(
        String id,
        String customerId,
        String customerName,
        List<DraftLineResponse> items,
        DiscountType discountType,
        Long discountValue,
        PaymentMethod paymentMethod,
        String note,
        Instant createdAt,
        Instant savedAt) {}
