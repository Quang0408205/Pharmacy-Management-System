package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.Role;

public record ActivationInfoResponse(String name, String username, String email, Role role) {}
