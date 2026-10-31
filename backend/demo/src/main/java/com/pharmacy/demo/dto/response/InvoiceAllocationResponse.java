package com.pharmacy.demo.dto.response;

import java.time.LocalDate;

public record InvoiceAllocationResponse(String batchId, String batchCode, LocalDate expiryDate, Integer quantity) {}
