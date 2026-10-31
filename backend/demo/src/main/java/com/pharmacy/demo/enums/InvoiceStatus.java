package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvoiceStatus implements CodedEnum {
    COMPLETED("HOAN_THANH"),
    CANCELLED("DA_HUY");

    private final String dbCode;
}
