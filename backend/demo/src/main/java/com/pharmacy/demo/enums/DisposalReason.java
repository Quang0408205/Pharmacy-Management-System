package com.pharmacy.demo.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DisposalReason implements CodedEnum {
    EXPIRED("HET_HAN"),
    DAMAGED("HU_HONG"),
    RECALLED("THU_HOI");

    private final String dbCode;
}
