package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PrescriptionType implements CodedEnum {
    OTC("KHONG_KE_DON"),
    RX("KE_DON"),
    CONTROLLED("KIEM_SOAT_DAC_BIET"),
    UNVERIFIED("CHUA_XAC_MINH");

    private final String dbCode;
}
