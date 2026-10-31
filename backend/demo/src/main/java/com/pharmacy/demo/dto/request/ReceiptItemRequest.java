package com.pharmacy.demo.dto.request;

import java.time.LocalDate;

public record ReceiptItemRequest(
        String medicineId,
        String batchCode,
        LocalDate manufactureDate,
        LocalDate expiryDate,
        Integer quantity,
        Long purchasePrice) {}
