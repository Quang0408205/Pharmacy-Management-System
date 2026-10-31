package com.pharmacy.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Mã danh mục là bắt buộc.")
        @Size(max = 20, message = "Mã danh mục tối đa 20 ký tự.")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]*$",
                message = "Mã danh mục chỉ gồm chữ, số, dấu chấm, gạch dưới, gạch ngang.")
        String code,

        @NotBlank(message = "Tên danh mục là bắt buộc.") @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự.")
        String name,

        @Size(max = 500, message = "Mô tả tối đa 500 ký tự.")
        String description,

        @Pattern(regexp = "^(#[0-9A-Fa-f]{6})?$", message = "Màu hiển thị phải có dạng #RRGGBB.")
        String color) {}
