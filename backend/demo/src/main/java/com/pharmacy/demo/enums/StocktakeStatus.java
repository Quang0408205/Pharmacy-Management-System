package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StocktakeStatus implements CodedEnum {
    DRAFT("NHAP"),
    PENDING_APPROVAL("CHO_DUYET"),
    APPROVED("DA_DUYET"),
    REJECTED("TU_CHOI");

    private final String dbCode;
}
