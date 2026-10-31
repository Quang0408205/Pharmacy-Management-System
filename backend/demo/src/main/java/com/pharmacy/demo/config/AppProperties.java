package com.pharmacy.demo.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String timezone, Security security, Seed seed, SystemReset systemReset, Storage storage) {

    public record Security(
            String jwtSecret,
            String cookieName,
            boolean cookieSecure,
            Duration sessionTtl,
            Duration rememberTtl,
            int maxFailedLogins,
            Duration lockDuration,
            Duration activationTtl,
            Duration resetTtl) {}

    public record Seed(boolean enabled, String defaultPassword, String dataDirectory) {}

    public record SystemReset(boolean enabled) {}

    public record Storage(String medicineImageDirectory) {}
}
