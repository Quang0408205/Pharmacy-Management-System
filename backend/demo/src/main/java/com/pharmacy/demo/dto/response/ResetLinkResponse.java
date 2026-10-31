package com.pharmacy.demo.dto.response;

import java.time.Instant;

public record ResetLinkResponse(String resetLink, Instant expiresAt) {}
