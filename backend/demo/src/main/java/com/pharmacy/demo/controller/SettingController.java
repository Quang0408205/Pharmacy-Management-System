package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.request.SettingsRequest;
import com.pharmacy.demo.dto.response.SettingsResponse;
import com.pharmacy.demo.mapper.SettingMapper;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.SystemSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cài đặt", description = "Cấu hình hệ thống và thông tin nhà thuốc.")
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SystemSettingService settingService;
    private final SettingMapper settingMapper;

    @Operation(summary = "Xem cài đặt")
    @GetMapping
    public SettingsResponse get() {
        return settingMapper.toResponse(settingService.current());
    }

    @Operation(summary = "Cập nhật cài đặt (Admin)")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SettingsResponse update(@Valid @RequestBody SettingsRequest request, @AuthenticationPrincipal Actor actor) {
        return settingMapper.toResponse(settingService.update(request, actor));
    }
}
