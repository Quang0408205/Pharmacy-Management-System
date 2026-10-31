package com.pharmacy.demo.dto.response;

import java.time.Instant;

public record EmployeeCreatedResponse(EmployeeResponse employee, String activationLink, Instant expiresAt) {}
