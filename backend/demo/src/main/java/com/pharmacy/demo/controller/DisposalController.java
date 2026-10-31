package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.ApproveRequest;
import com.pharmacy.demo.dto.request.DisposalRequest;
import com.pharmacy.demo.dto.request.RejectRequest;
import com.pharmacy.demo.dto.response.DisposalDetailResponse;
import com.pharmacy.demo.dto.response.DisposalSummaryResponse;
import com.pharmacy.demo.enums.DisposalStatus;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.DisposalCommandService;
import com.pharmacy.demo.service.DisposalService;
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

@Tag(name = "Xuất hủy", description = "Quy trình Kho đề xuất, Admin duyệt, Kho thực hiện xuất hủy thuốc.")
@RestController
@RequestMapping("/api/v1/disposals")
@RequiredArgsConstructor
public class DisposalController {

    private final DisposalService disposalService;
    private final DisposalCommandService commandService;

    @Operation(summary = "Danh sách phiếu xuất hủy")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public PageResponse<DisposalSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) DisposalStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return disposalService.search(q, status, PageQuery.of(page, size, sort));
    }

    @Operation(summary = "Chi tiết phiếu xuất hủy")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
    public DisposalDetailResponse get(@PathVariable String id) {
        return disposalService.get(id);
    }

    @Operation(summary = "Lập phiếu xuất hủy nháp (Kho)")
    @PostMapping
    @PreAuthorize("hasRole('WAREHOUSE')")
    public DisposalDetailResponse create(
            @Valid @RequestBody DisposalRequest request, @AuthenticationPrincipal Actor actor) {
        return commandService.create(request, actor);
    }

    @Operation(summary = "Sửa phiếu nháp (Kho)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public DisposalDetailResponse update(
            @PathVariable String id,
            @Valid @RequestBody DisposalRequest request,
            @AuthenticationPrincipal Actor actor) {
        return commandService.update(id, request, actor);
    }

    @Operation(summary = "Xóa phiếu nháp (Kho)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public Map<String, Boolean> delete(@PathVariable String id, @AuthenticationPrincipal Actor actor) {
        commandService.delete(id, actor);
        return Map.of("success", true);
    }

    @Operation(summary = "Gửi duyệt, các lô chuyển sang cách ly (Kho)")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public DisposalDetailResponse submit(@PathVariable String id, @AuthenticationPrincipal Actor actor) {
        return commandService.submit(id, actor);
    }

    @Operation(summary = "Thực hiện hủy sau khi được duyệt, trừ kho (Kho)")
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasRole('WAREHOUSE')")
    public DisposalDetailResponse execute(@PathVariable String id, @AuthenticationPrincipal Actor actor) {
        return commandService.execute(id, actor);
    }

    @Operation(summary = "Duyệt phiếu, chưa trừ kho (Admin)")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public DisposalDetailResponse approve(
            @PathVariable String id,
            @Valid @RequestBody(required = false) ApproveRequest request,
            @AuthenticationPrincipal Actor actor) {
        return disposalService.approve(id, request == null ? null : request.note(), actor);
    }

    @Operation(summary = "Từ chối phiếu, lô được trả về trạng thái bán (Admin)")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public DisposalDetailResponse reject(
            @PathVariable String id, @Valid @RequestBody RejectRequest request, @AuthenticationPrincipal Actor actor) {
        return disposalService.reject(id, request.note(), actor);
    }
}
