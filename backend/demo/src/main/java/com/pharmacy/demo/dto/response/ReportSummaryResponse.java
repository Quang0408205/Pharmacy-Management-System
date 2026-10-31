package com.pharmacy.demo.dto.response;

import java.time.LocalDate;

public record ReportSummaryResponse(
        LocalDate from, LocalDate to, long revenue, long invoices, long cancelled, long averageOrder, long itemsSold) {}
