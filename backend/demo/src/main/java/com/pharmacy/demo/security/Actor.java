package com.pharmacy.demo.security;

import com.pharmacy.demo.enums.Role;

public record Actor(String id, String name, Role role) {

    public static Actor system() {
        return new Actor(null, "Hệ thống", null);
    }
}
