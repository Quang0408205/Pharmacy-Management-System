package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.enums.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull(message = "Vai trò là bắt buộc.") Role role) {}
