package com.pharmacy.demo.dto.request;

import jakarta.validation.constraints.Size;

public record StocktakeLineRequest(
        String batchId,
        Integer actualQty,

        @Size(max = 300, message = "Lý do tối đa 300 ký tự.")
        String reason) {}
