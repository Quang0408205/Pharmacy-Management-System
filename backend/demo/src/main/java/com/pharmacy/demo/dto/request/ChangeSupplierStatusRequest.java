package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.enums.SupplierStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeSupplierStatusRequest(
        @NotNull(message = "Trạng thái là bắt buộc.") SupplierStatus status) {}
