package com.pharmacy.demo.service;

import com.pharmacy.demo.enums.BatchHandlingStatus;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.ReferenceType;
import com.pharmacy.demo.security.Actor;
import java.util.Set;
import lombok.Builder;

@Builder
public record StockChange(
        String batchId,
        int delta,
        MovementType type,
        ReferenceType referenceType,
        String referenceId,
        String referenceCode,
        Actor actor,
        String note,
        Long expectedVersion,
        Set<BatchHandlingStatus> allowedStatuses) {}
