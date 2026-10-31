package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.BatchResponse;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.enums.BatchStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BatchMapper {

    @Mapping(target = "medicineCode", source = "medicineCode")
    @Mapping(target = "medicineName", source = "medicineName")
    @Mapping(target = "unit", source = "unit")
    @Mapping(target = "supplierName", source = "supplierName")
    @Mapping(target = "receiptCode", source = "receiptCode")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "movements", ignore = true)
    BatchResponse toResponse(
            StockBatch batch,
            BatchStatus status,
            String medicineCode,
            String medicineName,
            String unit,
            String supplierName,
            String receiptCode);
}
