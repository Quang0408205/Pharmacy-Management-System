package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.enums.EmployeeStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
        @NotNull(message = "Trạng thái là bắt buộc.") EmployeeStatus status) {}
