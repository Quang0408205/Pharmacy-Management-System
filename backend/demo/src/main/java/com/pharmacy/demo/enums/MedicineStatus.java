package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MedicineStatus implements CodedEnum {
    ACTIVE("DANG_KINH_DOANH"),
    INACTIVE("NGUNG_KINH_DOANH");

    private final String dbCode;
}
