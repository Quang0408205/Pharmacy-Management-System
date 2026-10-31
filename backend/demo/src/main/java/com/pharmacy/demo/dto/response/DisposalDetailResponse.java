package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.DisposalStatus;
import java.time.Instant;
import java.util.List;

public record DisposalDetailResponse(
        String id,
        String code,
        DisposalStatus status,
        String reason,
        String note,
        String createdById,
        String createdByName,
        Instant createdAt,
        Instant submittedAt,
        String reviewedById,
        String reviewedByName,
        Instant reviewedAt,
        String reviewNote,
        String executedById,
        String executedByName,
        Instant executedAt,
        List<DisposalLineResponse> lines) {}
