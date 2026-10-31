package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.entity.VerificationToken;
import com.pharmacy.demo.enums.TokenType;
import com.pharmacy.demo.exception.InvalidTokenException;
import com.pharmacy.demo.repository.VerificationTokenRepository;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.IdGenerator;
import com.pharmacy.demo.service.VerificationTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerificationTokenServiceImpl implements VerificationTokenService {

    private static final int TOKEN_BYTES = 32;

    private final VerificationTokenRepository repository;
    private final MongoTemplate mongoTemplate;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public IssuedToken issue(String employeeId, TokenType type, Duration ttl, Actor creator) {
        repository.deleteAll(repository.findByEmployeeIdAndTypeAndUsedAtIsNull(employeeId, type));
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        repository.save(VerificationToken.builder()
                .id(idGenerator.newId())
                .employeeId(employeeId)
                .type(type)
                .tokenHash(hash(value))
                .createdAt(now)
                .expiresAt(expiresAt)
                .createdById(creator.id())
                .build());
        return new IssuedToken(value, expiresAt);
    }

    @Override
    public VerificationToken find(String rawToken, TokenType type) {
        Instant now = clock.instant();
        return repository
                .findByTokenHash(hash(rawToken))
                .filter(token -> token.getType() == type
                        && token.getUsedAt() == null
                        && token.getExpiresAt().isAfter(now))
                .orElseThrow(InvalidTokenException::new);
    }

    @Override
    public VerificationToken consume(String rawToken, TokenType type) {
        Instant now = clock.instant();
        Query query = Query.query(Criteria.where("tokenHash")
                .is(hash(rawToken))
                .and("type")
                .is(type)
                .and("usedAt")
                .is(null)
                .and("expiresAt")
                .gt(now));
        VerificationToken token = mongoTemplate.findAndModify(
                query,
                new Update().set("usedAt", now),
                FindAndModifyOptions.options().returnNew(true),
                VerificationToken.class);
        if (token == null) {
            throw new InvalidTokenException();
        }
        return token;
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
