package com.pharmacy.demo.dto.response;

public record StocktakeLineResponse(
        String batchId,
        String batchCode,
        String medicineId,
        String medicineName,
        Integer systemQty,
        Integer actualQty,
        Integer difference,
        String reason) {}
