package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod implements CodedEnum {
    CASH("TIEN_MAT"),
    TRANSFER("CHUYEN_KHOAN"),
    CARD("THE");

    private final String dbCode;
}
