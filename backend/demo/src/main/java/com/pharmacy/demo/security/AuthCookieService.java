package com.pharmacy.demo.security;

import com.pharmacy.demo.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthCookieService {

    private static final String COOKIE_PATH = "/api";

    private final AppProperties properties;

    public ResponseCookie issue(String token, Duration ttl) {
        return builder(token).maxAge(ttl).build();
    }

    public ResponseCookie clear() {
        return builder("").maxAge(Duration.ZERO).build();
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        String name = properties.security().cookieName();
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder builder(String value) {
        return ResponseCookie.from(properties.security().cookieName(), value)
                .httpOnly(true)
                .secure(properties.security().cookieSecure())
                .sameSite("Strict")
                .path(COOKIE_PATH);
    }
}
