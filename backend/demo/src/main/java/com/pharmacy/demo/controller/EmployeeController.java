package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.ChangeRoleRequest;
import com.pharmacy.demo.dto.request.ChangeStatusRequest;
import com.pharmacy.demo.dto.request.CreateEmployeeRequest;
import com.pharmacy.demo.dto.response.EmployeeCreatedResponse;
import com.pharmacy.demo.dto.response.EmployeeDetailResponse;
import com.pharmacy.demo.dto.response.EmployeeResponse;
import com.pharmacy.demo.dto.response.ResetLinkResponse;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.EmployeeService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Nhân viên", description = "Quản lý tài khoản nhân viên (Admin).")
@RestController
@RequestMapping("/api/v1/employees")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "Danh sách nhân viên")
    @GetMapping
    public PageResponse<EmployeeResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return employeeService.search(
                new EmployeeService.EmployeeFilter(q, role, status), PageQuery.of(page, size, sort));
    }

    @Operation(summary = "Chi tiết nhân viên")
    @GetMapping("/{id}")
    public EmployeeDetailResponse get(@PathVariable String id) {
        return employeeService.get(id);
    }

    @Operation(summary = "Tạo nhân viên và nhận link kích hoạt hiển thị một lần")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeCreatedResponse create(
            @Valid @RequestBody CreateEmployeeRequest request, @AuthenticationPrincipal Actor actor) {
        return employeeService.create(request, actor);
    }

    @Operation(summary = "Đổi vai trò nhân viên")
    @PatchMapping("/{id}")
    public EmployeeResponse changeRole(
            @PathVariable String id,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal Actor actor) {
        return employeeService.changeRole(id, request.role(), actor);
    }

    @Operation(summary = "Kích hoạt hoặc vô hiệu hóa nhân viên")
    @PatchMapping("/{id}/status")
    public EmployeeResponse changeStatus(
            @PathVariable String id,
            @Valid @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal Actor actor) {
        return employeeService.changeStatus(id, request.status(), actor);
    }

    @Operation(summary = "Tạo link đặt lại mật khẩu")
    @PostMapping("/{id}/reset-link")
    public ResetLinkResponse resetLink(@PathVariable String id, @AuthenticationPrincipal Actor actor) {
        return employeeService.issueResetLink(id, actor);
    }

    @Operation(summary = "Mở khóa tài khoản bị khóa do đăng nhập sai")
    @PostMapping("/{id}/unlock")
    public EmployeeResponse unlock(@PathVariable String id, @AuthenticationPrincipal Actor actor) {
        return employeeService.unlock(id, actor);
    }
}
