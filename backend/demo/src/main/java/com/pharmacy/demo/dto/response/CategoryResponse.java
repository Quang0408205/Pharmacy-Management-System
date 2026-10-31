package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.CategoryStatus;

public record CategoryResponse(
        String id,
        String code,
        String name,
        String description,
        String color,
        CategoryStatus status,
        long medicineCount) {}
