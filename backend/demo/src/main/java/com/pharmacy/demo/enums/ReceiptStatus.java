package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReceiptStatus implements CodedEnum {
    DRAFT("NHAP"),
    CONFIRMED("DA_XAC_NHAN");

    private final String dbCode;
}
