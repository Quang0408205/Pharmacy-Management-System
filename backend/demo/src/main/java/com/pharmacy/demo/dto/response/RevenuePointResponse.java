package com.pharmacy.demo.dto.response;

import java.time.LocalDate;

public record RevenuePointResponse(LocalDate date, long revenue, long invoices) {}
