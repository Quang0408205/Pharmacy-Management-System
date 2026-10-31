package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SupplierStatus implements CodedEnum {
    ACTIVE("HOAT_DONG"),
    INACTIVE("NGUNG_HOP_TAC");

    private final String dbCode;
}
