package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DisposalStatus implements CodedEnum {
    DRAFT("NHAP"),
    PENDING_APPROVAL("CHO_DUYET"),
    APPROVED("DA_DUYET"),
    REJECTED("TU_CHOI"),
    EXECUTED("DA_THUC_HIEN");

    private final String dbCode;
}
