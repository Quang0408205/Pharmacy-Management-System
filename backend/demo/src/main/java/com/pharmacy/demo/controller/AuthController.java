package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.request.ActivateAccountRequest;
import com.pharmacy.demo.dto.request.LoginRequest;
import com.pharmacy.demo.dto.request.ResetPasswordRequest;
import com.pharmacy.demo.dto.response.ActivationInfoResponse;
import com.pharmacy.demo.dto.response.AuthResponse;
import com.pharmacy.demo.exception.UnauthorizedException;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.security.AuthCookieService;
import com.pharmacy.demo.service.AccountService;
import com.pharmacy.demo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Xác thực",
        description = "Đăng nhập, đăng xuất, kích hoạt tài khoản và đặt lại mật khẩu bằng link do Admin cấp.")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountService accountService;
    private final AuthCookieService cookieService;

    @Operation(summary = "Đăng nhập bằng email hoặc tên đăng nhập, nhận cookie phiên")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieService.issue(result.token(), result.ttl()).toString())
                .body(new AuthResponse(result.user()));
    }

    @Operation(summary = "Đăng xuất và xóa cookie phiên")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.clear().toString())
                .body(Map.of("success", true));
    }

    @Operation(summary = "Thông tin người dùng đang đăng nhập")
    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal Actor actor) {
        if (actor == null) {
            throw new UnauthorizedException("UNAUTHORIZED", "Chưa đăng nhập.");
        }
        return new AuthResponse(authService.currentUser(actor.id()));
    }

    @Operation(summary = "Kiểm tra link kích hoạt tài khoản")
    @GetMapping("/activation/{token}")
    public ActivationInfoResponse activationInfo(@PathVariable String token) {
        return accountService.describeActivation(token);
    }

    @Operation(summary = "Đặt mật khẩu lần đầu bằng link kích hoạt")
    @PostMapping("/activate")
    public Map<String, Boolean> activate(@Valid @RequestBody ActivateAccountRequest request) {
        accountService.activate(request);
        return Map.of("success", true);
    }

    @Operation(summary = "Kiểm tra link đặt lại mật khẩu")
    @GetMapping("/reset-tokens/{token}")
    public Map<String, Boolean> validateResetToken(@PathVariable String token) {
        accountService.validateResetToken(token);
        return Map.of("valid", true);
    }

    @Operation(summary = "Đặt lại mật khẩu bằng link do Admin cấp")
    @PostMapping("/reset-password")
    public Map<String, Boolean> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(request);
        return Map.of("success", true);
    }
}
