package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmployeeStatus implements CodedEnum {
    PENDING("CHO_KICH_HOAT"),
    ACTIVE("HOAT_DONG"),
    INACTIVE("VO_HIEU_HOA");

    private final String dbCode;
}
