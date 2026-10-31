package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.ActivityResponse;
import com.pharmacy.demo.entity.ActivityLog;
import org.mapstruct.Mapper;

@Mapper
public interface ActivityLogMapper {

    ActivityResponse toResponse(ActivityLog log);
}
