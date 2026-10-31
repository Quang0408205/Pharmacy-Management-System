package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.MedicineResponse;
import com.pharmacy.demo.entity.Medicine;
import com.pharmacy.demo.enums.StockStatus;
import com.pharmacy.demo.service.StockSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MedicineMapper {

    @Mapping(target = "categoryName", source = "categoryName")
    @Mapping(target = "totalQty", source = "stock.totalQty")
    @Mapping(target = "availableQty", source = "stock.availableQty")
    @Mapping(target = "expiredQty", source = "stock.expiredQty")
    @Mapping(target = "batchCount", source = "stock.batchCount")
    @Mapping(target = "nearestExpiry", source = "stock.nearestExpiry")
    @Mapping(target = "stockStatus", source = "stockStatus")
    @Mapping(target = "batches", ignore = true)
    @Mapping(target = "movements", ignore = true)
    MedicineResponse toResponse(Medicine medicine, String categoryName, StockSummary stock, StockStatus stockStatus);
}
