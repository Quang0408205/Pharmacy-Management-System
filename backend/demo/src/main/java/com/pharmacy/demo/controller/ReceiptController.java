package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.ReceiptRequest;
import com.pharmacy.demo.dto.response.CodePreviewResponse;
import com.pharmacy.demo.dto.response.ReceiptDetailResponse;
import com.pharmacy.demo.dto.response.ReceiptSummaryResponse;
import com.pharmacy.demo.enums.ReceiptStatus;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.ReceiptCommandService;
import com.pharmacy.demo.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Phiếu nhập", description = "Nhập thuốc vào kho.")
@RestController
@RequestMapping("/api/v1/receipts")
@PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;
    private final ReceiptCommandService commandService;

    @Operation(summary = "Danh sách phiếu nhập")
    @GetMapping
    public PageResponse<ReceiptSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ReceiptStatus status,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return receiptService.search(
                new ReceiptService.ReceiptFilter(q, status, supplierId, from, to), PageQuery.of(page, size, sort));
    }

    @Operation(summary = "Xem trước mã phiếu nhập kế tiếp (Kho)")
    @GetMapping("/next-code")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public CodePreviewResponse nextCode() {
        return new CodePreviewResponse(commandService.nextCode());
    }

    @Operation(summary = "Lưu nháp hoặc xác nhận phiếu nhập; xác nhận tạo lô và ghi biến động (Kho)")
    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE')")
    public ReceiptDetailResponse create(
            @Valid @RequestBody ReceiptRequest request, @AuthenticationPrincipal Actor actor) {
        return commandService.create(request, actor);
    }

    @Operation(summary = "Sửa hoặc xác nhận phiếu nháp (Kho)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public ReceiptDetailResponse update(
            @PathVariable String id, @Valid @RequestBody ReceiptRequest request, @AuthenticationPrincipal Actor actor) {
        return commandService.update(id, request, actor);
    }

    @Operation(summary = "Xóa phiếu nháp (Kho)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public Map<String, Boolean> delete(@PathVariable String id, @AuthenticationPrincipal Actor actor) {
        commandService.delete(id, actor);
        return Map.of("success", true);
    }

    @Operation(summary = "Chi tiết phiếu nhập")
    @GetMapping("/{id}")
    public ReceiptDetailResponse get(@PathVariable String id) {
        return receiptService.get(id);
    }
}
