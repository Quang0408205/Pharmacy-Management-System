package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LoyaltyType implements CodedEnum {
    EARN("TICH_DIEM"),
    REDEEM("DOI_DIEM"),
    REDEEM_REFUND("HOAN_DIEM"),
    EARN_REVOKE("THU_HOI_DIEM");

    private final String dbCode;
}
