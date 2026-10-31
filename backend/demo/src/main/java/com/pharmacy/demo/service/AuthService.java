package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.request.LoginRequest;
import com.pharmacy.demo.dto.response.UserResponse;
import java.time.Duration;

public interface AuthService {

    LoginResult login(LoginRequest request);

    UserResponse currentUser(String employeeId);

    record LoginResult(UserResponse user, String token, Duration ttl) {}
}
