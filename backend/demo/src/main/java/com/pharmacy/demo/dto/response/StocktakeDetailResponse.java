package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.StocktakeStatus;
import java.time.Instant;
import java.util.List;

public record StocktakeDetailResponse(
        String id,
        String code,
        StocktakeStatus status,
        String scope,
        String scopeLabel,
        String note,
        String createdById,
        String createdByName,
        Instant createdAt,
        Instant submittedAt,
        String reviewedById,
        String reviewedByName,
        Instant reviewedAt,
        String reviewNote,
        List<StocktakeLineResponse> lines) {}
