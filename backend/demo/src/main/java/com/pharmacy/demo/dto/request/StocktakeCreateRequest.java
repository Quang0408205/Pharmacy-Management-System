package com.pharmacy.demo.dto.request;

import jakarta.validation.constraints.Size;

public record StocktakeCreateRequest(
        String categoryId,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
        String note) {}
