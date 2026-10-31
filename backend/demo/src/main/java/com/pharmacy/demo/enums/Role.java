package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role implements CodedEnum {
    ADMIN("QUAN_TRI"),
    SALES("BAN_THUOC"),
    WAREHOUSE("KHO");

    private final String dbCode;
}
