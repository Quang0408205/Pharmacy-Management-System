package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.Role;
import java.time.Instant;

public record ActivityResponse(
        String id,
        Instant createdAt,
        String actorId,
        String actorName,
        Role role,
        String action,
        String entity,
        String entityCode,
        String description) {}
