package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.request.ChangePasswordRequest;
import com.pharmacy.demo.dto.request.LoginRequest;
import com.pharmacy.demo.dto.response.UserResponse;
import java.time.Duration;

public interface AuthService {

    LoginResult login(LoginRequest request);

    UserResponse currentUser(String employeeId);

    LoginResult changePassword(String employeeId, ChangePasswordRequest request);

    record LoginResult(UserResponse user, String token, Duration ttl) {}
}
