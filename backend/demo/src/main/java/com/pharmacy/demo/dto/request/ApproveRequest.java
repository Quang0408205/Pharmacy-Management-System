package com.pharmacy.demo.dto.request;

import jakarta.validation.constraints.Size;

public record ApproveRequest(
        @Size(max = 500, message = "Ý kiến tối đa 500 ký tự.")
        String note) {}
