package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.ChangeSupplierStatusRequest;
import com.pharmacy.demo.dto.request.SupplierRequest;
import com.pharmacy.demo.dto.response.SupplierDetailResponse;
import com.pharmacy.demo.dto.response.SupplierResponse;
import com.pharmacy.demo.enums.SupplierStatus;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Nhà cung cấp", description = "Quản lý nhà cung cấp thuốc.")
@RestController
@RequestMapping("/api/v1/suppliers")
@PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "Danh sách nhà cung cấp")
    @GetMapping
    public PageResponse<SupplierResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SupplierStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return supplierService.search(q, status, PageQuery.of(page, size, sort));
    }

    @Operation(summary = "Chi tiết nhà cung cấp kèm phiếu nhập")
    @GetMapping("/{id}")
    public SupplierDetailResponse get(@PathVariable String id) {
        return supplierService.get(id);
    }

    @Operation(summary = "Tạo nhà cung cấp")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse create(@Valid @RequestBody SupplierRequest request, @AuthenticationPrincipal Actor actor) {
        return supplierService.create(request, actor);
    }

    @Operation(summary = "Sửa nhà cung cấp")
    @PutMapping("/{id}")
    public SupplierResponse update(
            @PathVariable String id,
            @Valid @RequestBody SupplierRequest request,
            @AuthenticationPrincipal Actor actor) {
        return supplierService.update(id, request, actor);
    }

    @Operation(summary = "Đổi trạng thái hợp tác")
    @PatchMapping("/{id}/status")
    public SupplierResponse changeStatus(
            @PathVariable String id,
            @Valid @RequestBody ChangeSupplierStatusRequest request,
            @AuthenticationPrincipal Actor actor) {
        return supplierService.changeStatus(id, request.status(), actor);
    }
}
