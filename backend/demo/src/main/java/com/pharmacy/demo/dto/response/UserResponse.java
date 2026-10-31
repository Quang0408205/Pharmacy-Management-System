package com.pharmacy.demo.dto.response;

import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.Role;

public record UserResponse(
        String id, String name, String email, String username, Role role, String phone, EmployeeStatus status) {}
