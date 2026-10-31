package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.SupplierStatus;
import java.time.Instant;
import java.util.List;

public record SupplierDetailResponse(
        String id,
        String code,
        String name,
        String contactName,
        String phone,
        String email,
        String address,
        String taxCode,
        String paymentTerms,
        String note,
        SupplierStatus status,
        Instant createdAt,
        long receiptCount,
        long totalAmount,
        List<ReceiptSummaryResponse> receipts) {}
