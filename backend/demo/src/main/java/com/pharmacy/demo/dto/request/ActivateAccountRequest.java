package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.util.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActivateAccountRequest(
        @NotBlank(message = "Mã xác thực là bắt buộc.") String token,

        @NotBlank(message = "Mật khẩu là bắt buộc.")
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
        String password) {}
