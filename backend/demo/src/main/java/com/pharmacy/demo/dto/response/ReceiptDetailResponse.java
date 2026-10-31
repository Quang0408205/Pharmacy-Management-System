package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.ReceiptStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ReceiptDetailResponse(
        String id,
        String code,
        String supplierId,
        String supplierName,
        String createdById,
        String createdByName,
        LocalDate receivingDate,
        List<ReceiptLineResponse> items,
        Integer totalQuantity,
        Long totalAmount,
        ReceiptStatus status,
        String note,
        Instant createdAt,
        Instant confirmedAt) {}
