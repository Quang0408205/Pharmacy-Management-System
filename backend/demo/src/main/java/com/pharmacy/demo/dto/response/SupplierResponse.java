package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.SupplierStatus;
import java.time.Instant;

public record SupplierResponse(
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
        long receiptCount) {}
