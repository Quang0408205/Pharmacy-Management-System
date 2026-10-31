package com.pharmacy.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.BatchStatus;
import java.time.LocalDate;
import java.util.List;

public record BatchResponse(
        String id,
        String code,
        String medicineId,
        String medicineCode,
        String medicineName,
        String unit,
        String supplierId,
        String supplierName,
        String receiptId,
        String receiptCode,
        LocalDate receivedDate,
        LocalDate manufactureDate,
        LocalDate expiryDate,
        int initialQty,
        int remainingQty,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long purchasePrice,
        BatchHandlingStatus handlingStatus,
        BatchStatus status,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<MovementResponse> movements) {

    public BatchResponse withoutCost() {
        return new BatchResponse(
                id,
                code,
                medicineId,
                medicineCode,
                medicineName,
                unit,
                supplierId,
                supplierName,
                receiptId,
                receiptCode,
                receivedDate,
                manufactureDate,
                expiryDate,
                initialQty,
                remainingQty,
                null,
                handlingStatus,
                status,
                movements);
    }

    public static BatchResponse withMovements(BatchResponse source, List<MovementResponse> movementList) {
        return new BatchResponse(
                source.id(),
                source.code(),
                source.medicineId(),
                source.medicineCode(),
                source.medicineName(),
                source.unit(),
                source.supplierId(),
                source.supplierName(),
                source.receiptId(),
                source.receiptCode(),
                source.receivedDate(),
                source.manufactureDate(),
                source.expiryDate(),
                source.initialQty(),
                source.remainingQty(),
                source.purchasePrice(),
                source.handlingStatus(),
                source.status(),
                movementList);
    }
}
