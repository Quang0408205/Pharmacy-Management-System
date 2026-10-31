package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiscountType implements CodedEnum {
    AMOUNT("SO_TIEN"),
    PERCENT("PHAN_TRAM");

    private final String dbCode;
}
