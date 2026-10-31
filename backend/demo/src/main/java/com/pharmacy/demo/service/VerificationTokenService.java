package com.pharmacy.demo.service;

import com.pharmacy.demo.entity.VerificationToken;
import com.pharmacy.demo.enums.TokenType;
import com.pharmacy.demo.security.Actor;
import java.time.Duration;
import java.time.Instant;

public interface VerificationTokenService {

    IssuedToken issue(String employeeId, TokenType type, Duration ttl, Actor creator);

    VerificationToken find(String rawToken, TokenType type);

    VerificationToken consume(String rawToken, TokenType type);

    record IssuedToken(String value, Instant expiresAt) {}
}
