package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.BatchResponse;
import com.pharmacy.demo.enums.BatchStatus;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.BatchQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Lô thuốc", description = "Tra cứu lô thuốc, hạn dùng và tồn theo lô.")
@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchQueryService batchQueryService;

    @Operation(summary = "Danh sách lô, lọc theo thuốc, nhà cung cấp, trạng thái, hạn dùng")
    @GetMapping
    public PageResponse<BatchResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String medicineId,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) BatchStatus status,
            @RequestParam(required = false) Integer expiryWithin,
            @RequestParam(defaultValue = "false") boolean hasStock,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal Actor actor) {
        BatchQueryService.BatchFilter filter =
                new BatchQueryService.BatchFilter(q, medicineId, supplierId, status, expiryWithin, hasStock);
        return batchQueryService.search(filter, PageQuery.of(page, size, sort), actor);
    }

    @Operation(summary = "Chi tiết lô kèm lịch sử biến động")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public BatchResponse get(@PathVariable String id) {
        return batchQueryService.get(id);
    }
}
