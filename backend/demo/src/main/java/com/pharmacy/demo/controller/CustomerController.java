package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.CustomerRequest;
import com.pharmacy.demo.dto.response.CustomerDetailResponse;
import com.pharmacy.demo.dto.response.CustomerResponse;
import com.pharmacy.demo.dto.response.LoyaltyEntryResponse;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.CustomerService;
import com.pharmacy.demo.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Khách hàng", description = "Hồ sơ khách hàng mua thuốc.")
@RestController
@RequestMapping("/api/v1/customers")
@PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final LoyaltyService loyaltyService;

    @Operation(summary = "Danh sách khách hàng kèm số hóa đơn và tổng chi tiêu")
    @GetMapping
    public PageResponse<CustomerResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return customerService.search(q, PageQuery.of(page, size, sort));
    }

    @Operation(summary = "Tìm khách hàng theo số điện thoại")
    @GetMapping("/by-phone")
    public CustomerResponse byPhone(@RequestParam String phone) {
        return customerService.findByPhone(phone);
    }

    @Operation(summary = "Lịch sử điểm tích lũy của khách hàng")
    @GetMapping("/{id}/loyalty")
    public List<LoyaltyEntryResponse> loyalty(@PathVariable String id) {
        return loyaltyService.history(id);
    }

    @Operation(summary = "Chi tiết khách hàng kèm hóa đơn gần đây")
    @GetMapping("/{id}")
    public CustomerDetailResponse get(@PathVariable String id) {
        return customerService.get(id);
    }

    @Operation(summary = "Tạo khách hàng")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request, @AuthenticationPrincipal Actor actor) {
        return customerService.create(request, actor);
    }

    @Operation(summary = "Sửa khách hàng")
    @PutMapping("/{id}")
    public CustomerResponse update(
            @PathVariable String id,
            @Valid @RequestBody CustomerRequest request,
            @AuthenticationPrincipal Actor actor) {
        return customerService.update(id, request, actor);
    }
}
