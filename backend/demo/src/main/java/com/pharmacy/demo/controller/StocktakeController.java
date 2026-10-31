package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.ApproveRequest;
import com.pharmacy.demo.dto.request.RejectRequest;
import com.pharmacy.demo.dto.request.StocktakeCreateRequest;
import com.pharmacy.demo.dto.request.StocktakeSaveRequest;
import com.pharmacy.demo.dto.response.StocktakeDetailResponse;
import com.pharmacy.demo.dto.response.StocktakeSummaryResponse;
import com.pharmacy.demo.enums.StocktakeStatus;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.StocktakeCommandService;
import com.pharmacy.demo.service.StocktakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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

@Tag(name = "Kiểm kê", description = "Kiểm kê tồn kho: Kho lập phiếu, Admin duyệt điều chỉnh.")
@RestController
@RequestMapping("/api/v1/stocktakes")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;
    private final StocktakeCommandService commandService;

    @Operation(summary = "Danh sách phiếu kiểm kê")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public PageResponse<StocktakeSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StocktakeStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return stocktakeService.search(q, status, PageQuery.of(page, size, sort));
    }

    @Operation(summary = "Chi tiết phiếu kiểm kê")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public StocktakeDetailResponse get(@PathVariable String id) {
        return stocktakeService.get(id);
    }

    @Operation(summary = "Tạo phiên kiểm kê, chụp tồn tối đa 60 lô (Kho)")
    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE')")
    public StocktakeDetailResponse create(
            @Valid @RequestBody StocktakeCreateRequest request, @AuthenticationPrincipal Actor actor) {
        return commandService.create(request, actor);
    }

    @Operation(summary = "Lưu số liệu thực tế (Kho)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public StocktakeDetailResponse save(
            @PathVariable String id,
            @Valid @RequestBody(required = false) StocktakeSaveRequest request,
            @AuthenticationPrincipal Actor actor) {
        return commandService.save(id, request, actor);
    }

    @Operation(summary = "Xóa phiếu nháp (Kho)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public Map<String, Boolean> delete(@PathVariable String id, @AuthenticationPrincipal Actor actor) {
        commandService.delete(id, actor);
        return Map.of("success", true);
    }

    @Operation(summary = "Gửi duyệt khi đã nhập đủ số liệu (Kho)")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public StocktakeDetailResponse submit(
            @PathVariable String id,
            @Valid @RequestBody(required = false) StocktakeSaveRequest request,
            @AuthenticationPrincipal Actor actor) {
        return commandService.submit(id, request, actor);
    }

    @Operation(summary = "Duyệt và điều chỉnh tồn, từ chối nếu lô đã thay đổi (Admin)")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public StocktakeDetailResponse approve(
            @PathVariable String id,
            @Valid @RequestBody(required = false) ApproveRequest request,
            @AuthenticationPrincipal Actor actor) {
        return stocktakeService.approve(id, request == null ? null : request.note(), actor);
    }

    @Operation(summary = "Từ chối phiếu, bắt buộc ghi chú (Admin)")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public StocktakeDetailResponse reject(
            @PathVariable String id, @Valid @RequestBody RejectRequest request, @AuthenticationPrincipal Actor actor) {
        return stocktakeService.reject(id, request.note(), actor);
    }
}
