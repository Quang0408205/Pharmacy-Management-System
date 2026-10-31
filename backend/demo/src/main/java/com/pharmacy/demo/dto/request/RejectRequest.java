package com.pharmacy.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequest(
        @NotBlank(message = "Lý do từ chối là bắt buộc.") @Size(max = 500, message = "Lý do tối đa 500 ký tự.")
        String note) {}
