package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.ReferenceType;
import java.time.Instant;

public record MovementResponse(
        String id,
        Instant createdAt,
        MovementType type,
        String medicineId,
        String medicineName,
        String batchId,
        String batchCode,
        Integer quantity,
        Integer balanceAfter,
        ReferenceType referenceType,
        String referenceCode,
        String performedById,
        String performedByName,
        String note) {}
