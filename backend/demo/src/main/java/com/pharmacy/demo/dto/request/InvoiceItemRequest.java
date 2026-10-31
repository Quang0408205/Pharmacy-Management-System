package com.pharmacy.demo.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvoiceItemRequest(
        @NotBlank(message = "Thuốc là bắt buộc.") String medicineId,

        @NotNull(message = "Số lượng là bắt buộc.")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0.")
        @Max(value = 100000, message = "Số lượng quá lớn.")
        Integer quantity) {}
