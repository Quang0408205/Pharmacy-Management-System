package com.pharmacy.demo.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CodeKind {
    EMPLOYEE("NV", 3, Scope.NONE),
    MEDICINE("T", 4, Scope.NONE),
    SUPPLIER("NCC", 3, Scope.NONE),
    CUSTOMER("KH", 4, Scope.NONE),
    RECEIPT("PN", 4, Scope.YEAR),
    STOCKTAKE("KK", 3, Scope.YEAR),
    DISPOSAL("XH", 3, Scope.YEAR),
    INVOICE("HD", 4, Scope.DAY);

    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String prefix;
    private final int digits;
    private final Scope scope;

    public String counterKey(LocalDate date) {
        return switch (scope) {
            case NONE -> prefix;
            case YEAR -> prefix + "-" + YEAR.format(date);
            case DAY -> prefix + "-" + DAY.format(date);
        };
    }

    public String format(LocalDate date, long sequence) {
        String number = String.format("%0" + digits + "d", sequence);
        return switch (scope) {
            case NONE -> prefix + number;
            case YEAR -> prefix + "-" + YEAR.format(date) + "-" + number;
            case DAY -> prefix + "-" + DAY.format(date) + "-" + number;
        };
    }

    private enum Scope {
        NONE,
        YEAR,
        DAY
    }
}
