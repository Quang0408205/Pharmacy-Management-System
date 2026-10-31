package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.LoyaltyEntryResponse;
import com.pharmacy.demo.entity.LoyaltyTransaction;
import org.mapstruct.Mapper;

@Mapper
public interface LoyaltyMapper {

    LoyaltyEntryResponse toResponse(LoyaltyTransaction transaction);
}
