package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BatchHandlingStatus implements CodedEnum {
    AVAILABLE("KHA_DUNG"),
    QUARANTINED("CACH_LY"),
    DISPOSED("DA_XUAT_HUY");

    private final String dbCode;
}
