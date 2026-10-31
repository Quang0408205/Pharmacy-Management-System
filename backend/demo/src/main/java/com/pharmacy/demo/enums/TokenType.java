package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType implements CodedEnum {
    ACTIVATION("KICH_HOAT"),
    PASSWORD_RESET("DAT_LAI_MAT_KHAU");

    private final String dbCode;
}
