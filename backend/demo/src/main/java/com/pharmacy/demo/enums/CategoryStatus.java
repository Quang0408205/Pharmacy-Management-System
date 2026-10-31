package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryStatus implements CodedEnum {
    ACTIVE("HOAT_DONG"),
    INACTIVE("NGUNG_HOAT_DONG");

    private final String dbCode;
}
