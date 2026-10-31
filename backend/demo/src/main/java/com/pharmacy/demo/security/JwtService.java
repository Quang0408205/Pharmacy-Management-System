package com.pharmacy.demo.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.pharmacy.demo.config.AppProperties;
import com.pharmacy.demo.entity.Employee;
import com.pharmacy.demo.enums.Role;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final String ROLE_CLAIM = "role";
    private static final String SECURITY_VERSION_CLAIM = "secVer";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;

    public JwtService(AppProperties properties, Clock clock) {
        byte[] secret = properties.security().jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MINIMUM_SECRET_BYTES + " bytes long");
        }
        SecretKey key = new SecretKeySpec(secret, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        this.clock = clock;
    }

    public String issue(Employee employee, Duration ttl) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(employee.getId())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim(ROLE_CLAIM, employee.getRole().name())
                .claim(SECURITY_VERSION_CLAIM, employee.getSecurityVersion())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Optional<TokenClaims> parse(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            Long securityVersion = jwt.getClaim(SECURITY_VERSION_CLAIM);
            if (jwt.getSubject() == null || securityVersion == null) {
                return Optional.empty();
            }
            return Optional.of(
                    new TokenClaims(jwt.getSubject(), Role.valueOf(jwt.getClaimAsString(ROLE_CLAIM)), securityVersion));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public record TokenClaims(String subject, Role role, long securityVersion) {}
}
