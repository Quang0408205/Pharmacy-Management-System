package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MovementType implements CodedEnum {
    IN("NHAP"),
    OUT("XUAT_BAN"),
    ADJUSTMENT("DIEU_CHINH"),
    WRITE_OFF("XUAT_HUY"),
    RETURN("HOAN_TRA");

    private final String dbCode;
}
