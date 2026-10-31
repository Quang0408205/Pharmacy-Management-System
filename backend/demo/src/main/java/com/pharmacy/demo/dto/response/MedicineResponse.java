package com.pharmacy.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.StockStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MedicineResponse(
        String id,
        String code,
        String barcode,
        String name,
        String genericName,
        String strength,
        String dosageForm,
        String unit,
        String packaging,
        String manufacturer,
        String country,
        String registrationNumber,
        String categoryId,
        String categoryName,
        Long price,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long costPrice,
        PrescriptionType prescriptionType,
        MedicineStatus status,
        Integer minStock,
        String description,
        String imageUrl,
        Instant createdAt,
        int totalQty,
        int availableQty,
        int expiredQty,
        int batchCount,
        LocalDate nearestExpiry,
        StockStatus stockStatus,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<BatchResponse> batches,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<MovementResponse> movements) {

    public MedicineResponse withoutCost() {
        return new MedicineResponse(
                id,
                code,
                barcode,
                name,
                genericName,
                strength,
                dosageForm,
                unit,
                packaging,
                manufacturer,
                country,
                registrationNumber,
                categoryId,
                categoryName,
                price,
                null,
                prescriptionType,
                status,
                minStock,
                description,
                imageUrl,
                createdAt,
                totalQty,
                availableQty,
                expiredQty,
                batchCount,
                nearestExpiry,
                stockStatus,
                batches,
                movements);
    }

    public MedicineResponse withDetails(List<BatchResponse> batchList, List<MovementResponse> movementList) {
        return new MedicineResponse(
                id,
                code,
                barcode,
                name,
                genericName,
                strength,
                dosageForm,
                unit,
                packaging,
                manufacturer,
                country,
                registrationNumber,
                categoryId,
                categoryName,
                price,
                costPrice,
                prescriptionType,
                status,
                minStock,
                description,
                imageUrl,
                createdAt,
                totalQty,
                availableQty,
                expiredQty,
                batchCount,
                nearestExpiry,
                stockStatus,
                batchList,
                movementList);
    }
}
