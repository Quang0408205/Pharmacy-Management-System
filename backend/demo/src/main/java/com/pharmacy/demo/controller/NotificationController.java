package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.response.NotificationResponse;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Thông báo", description = "Cảnh báo tồn thấp và hạn dùng theo cấu hình.")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Danh sách thông báo cho người đang đăng nhập")
    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal Actor actor) {
        return notificationService.list(actor);
    }
}
