package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.DiscountType;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.PaymentMethod;
import java.time.Instant;

public record InvoiceSummaryResponse(
        String id,
        String code,
        String employeeId,
        String employeeName,
        String customerId,
        String customerName,
        int itemCount,
        int quantityTotal,
        Long subtotal,
        DiscountType discountType,
        Long discountValue,
        Long discountAmount,
        int pointsUsed,
        long pointsDiscount,
        int pointsEarned,
        Long total,
        PaymentMethod paymentMethod,
        Long receivedAmount,
        Long changeAmount,
        boolean prescriptionConfirmed,
        String prescriptionCode,
        InvoiceStatus status,
        String note,
        String cancelReason,
        Instant cancelledAt,
        Instant createdAt) {}
