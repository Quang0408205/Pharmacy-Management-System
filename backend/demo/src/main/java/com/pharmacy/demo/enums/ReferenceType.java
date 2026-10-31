package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReferenceType implements CodedEnum {
    RECEIPT("PHIEU_NHAP"),
    INVOICE("HOA_DON"),
    INVOICE_CANCEL("HUY_HOA_DON"),
    STOCKTAKE("KIEM_KE"),
    DISPOSAL("XUAT_HUY");

    private final String dbCode;
}
