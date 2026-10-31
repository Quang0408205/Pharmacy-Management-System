package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.request.SaveDraftRequest;
import com.pharmacy.demo.dto.response.InvoiceDraftResponse;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.InvoiceDraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Đơn nháp", description = "Giỏ bán lưu tạm của nhân viên bán thuốc.")
@RestController
@RequestMapping("/api/v1/invoice-drafts")
@PreAuthorize("hasRole('SALES')")
@RequiredArgsConstructor
public class InvoiceDraftController {

    private final InvoiceDraftService draftService;

    @Operation(summary = "Danh sách đơn nháp của mình")
    @GetMapping
    public List<InvoiceDraftResponse> list(@AuthenticationPrincipal Actor actor) {
        return draftService.list(actor);
    }

    @Operation(summary = "Tạo hoặc cập nhật đơn nháp")
    @PostMapping
    public InvoiceDraftResponse save(
            @Valid @RequestBody SaveDraftRequest request, @AuthenticationPrincipal Actor actor) {
        return draftService.save(request, actor);
    }

    @Operation(summary = "Xóa đơn nháp của mình")
    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@PathVariable String id, @AuthenticationPrincipal Actor actor) {
        draftService.delete(id, actor);
        return Map.of("success", true);
    }
}
