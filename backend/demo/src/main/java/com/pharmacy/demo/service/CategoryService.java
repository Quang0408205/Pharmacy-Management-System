package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.request.CategoryRequest;
import com.pharmacy.demo.dto.response.CategoryResponse;
import com.pharmacy.demo.security.Actor;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> list();

    CategoryResponse create(CategoryRequest request, Actor actor);

    CategoryResponse update(String id, CategoryRequest request, Actor actor);

    void delete(String id, Actor actor);
}
