package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.MovementResponse;
import com.pharmacy.demo.entity.StockMovement;
import org.mapstruct.Mapper;

@Mapper
public interface MovementMapper {

    MovementResponse toResponse(StockMovement movement);
}
