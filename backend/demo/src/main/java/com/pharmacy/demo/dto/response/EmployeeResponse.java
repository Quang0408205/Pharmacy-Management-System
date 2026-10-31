package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.Role;
import java.time.Instant;

public record EmployeeResponse(
        String id,
        String code,
        String name,
        String email,
        String username,
        String phone,
        Role role,
        EmployeeStatus status,
        boolean locked,
        Instant lockedUntil,
        Instant createdAt,
        Instant lastLoginAt,
        long invoiceCount) {}
