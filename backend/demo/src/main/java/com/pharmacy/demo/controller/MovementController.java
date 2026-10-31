package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.MovementResponse;
import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.service.MovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Biến động kho", description = "Sổ biến động tồn kho chỉ ghi thêm.")
@RestController
@RequestMapping("/api/v1/movements")
@PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
@RequiredArgsConstructor
public class MovementController {

    private final MovementService movementService;

    @Operation(summary = "Danh sách biến động, lọc theo loại, thuốc, lô, thời gian")
    @GetMapping
    public PageResponse<MovementResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) String medicineId,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return movementService.search(
                new MovementService.MovementFilter(q, type, medicineId, batchId, from, to),
                PageQuery.of(page, size, sort));
    }
}
