package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.enums.MedicineStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeMedicineStatusRequest(
        @NotNull(message = "Trạng thái là bắt buộc.") MedicineStatus status) {}
