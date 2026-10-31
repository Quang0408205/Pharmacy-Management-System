package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.CategoryResponse;
import com.pharmacy.demo.entity.Category;
import org.mapstruct.Mapper;

@Mapper
public interface CategoryMapper {

    CategoryResponse toResponse(Category category, long medicineCount);
}
