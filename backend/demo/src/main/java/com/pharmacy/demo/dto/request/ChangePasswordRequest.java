package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.util.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank(message = "Mật khẩu hiện tại là bắt buộc.")
        String currentPassword,

        @NotBlank(message = "Mật khẩu mới là bắt buộc.")
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
        String newPassword) {}
