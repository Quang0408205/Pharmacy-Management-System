package com.pharmacy.demo.controller;

import com.pharmacy.demo.bootstrap.DataSeeder;
import com.pharmacy.demo.config.AppProperties;
import com.pharmacy.demo.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Hệ thống", description = "Thao tác hệ thống, chỉ bật ở môi trường phát triển.")
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final AppProperties properties;
    private final DataSeeder dataSeeder;

    @Operation(summary = "Xóa và nạp lại dữ liệu mẫu (Admin, chỉ môi trường dev)")
    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Boolean> reset() {
        if (!properties.systemReset().enabled()) {
            throw new NotFoundException("Không tìm thấy API yêu cầu.");
        }
        dataSeeder.reset();
        return Map.of("success", true);
    }
}
