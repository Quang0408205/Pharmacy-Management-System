package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        @NotBlank(message = "Họ tên là bắt buộc.") @Size(max = 100, message = "Họ tên tối đa 100 ký tự.")
        String name,

        @NotBlank(message = "Email là bắt buộc.")
        @Email(message = "Email không hợp lệ.")
        @Size(max = 150, message = "Email tối đa 150 ký tự.")
        String email,

        @NotBlank(message = "Tên đăng nhập là bắt buộc.")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]{3,30}$",
                message = "Tên đăng nhập gồm 3-30 ký tự chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.")
        String username,

        @Pattern(regexp = "^(\\+?\\d[\\d ]{7,14})?$", message = "Số điện thoại không hợp lệ.")
        String phone,

        @NotNull(message = "Vai trò là bắt buộc.") Role role) {}
