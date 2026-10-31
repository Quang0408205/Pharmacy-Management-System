package com.pharmacy.demo.dto.response;

import java.time.LocalDate;

public record ReceiptLineResponse(
        Integer lineNo,
        String medicineId,
        String medicineCode,
        String medicineName,
        String batchId,
        String batchCode,
        LocalDate manufactureDate,
        LocalDate expiryDate,
        Integer quantity,
        String unit,
        Long purchasePrice,
        Long subtotal) {}
