package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.request.CategoryRequest;
import com.pharmacy.demo.dto.response.CategoryResponse;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Danh mục thuốc", description = "Quản lý nhóm danh mục thuốc.")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Danh sách danh mục")
    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.list();
    }

    @Operation(summary = "Tạo danh mục (Admin)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request, @AuthenticationPrincipal Actor actor) {
        return categoryService.create(request, actor);
    }

    @Operation(summary = "Sửa danh mục (Admin)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal Actor actor) {
        return categoryService.update(id, request, actor);
    }

    @Operation(summary = "Xóa danh mục chưa có thuốc (Admin)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Boolean> delete(@PathVariable String id, @AuthenticationPrincipal Actor actor) {
        categoryService.delete(id, actor);
        return Map.of("success", true);
    }
}
