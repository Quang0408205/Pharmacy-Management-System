package com.pharmacy.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email hoặc tên đăng nhập là bắt buộc.")
        String identifier,

        @NotBlank(message = "Mật khẩu là bắt buộc.") String password,
        Boolean remember) {}
