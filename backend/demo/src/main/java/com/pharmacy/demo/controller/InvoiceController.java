package com.pharmacy.demo.controller;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.CancelInvoiceRequest;
import com.pharmacy.demo.dto.request.CreateInvoiceRequest;
import com.pharmacy.demo.dto.response.CodePreviewResponse;
import com.pharmacy.demo.dto.response.InvoiceDetailResponse;
import com.pharmacy.demo.dto.response.InvoiceSummaryResponse;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.PaymentMethod;
import com.pharmacy.demo.security.Actor;
import com.pharmacy.demo.service.CheckoutService;
import com.pharmacy.demo.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Hóa đơn", description = "Bán thuốc tại quầy, tra cứu và hủy hóa đơn.")
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final CheckoutService checkoutService;

    @Operation(summary = "Danh sách hóa đơn, nhân viên bán chỉ thấy hóa đơn của mình")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public PageResponse<InvoiceSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String employeeId,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal Actor actor) {
        InvoiceService.InvoiceFilter filter =
                new InvoiceService.InvoiceFilter(q, employeeId, mine, status, paymentMethod, from, to);
        return invoiceService.search(filter, PageQuery.of(page, size, sort), actor);
    }

    @Operation(summary = "Chi tiết hóa đơn")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    public InvoiceDetailResponse get(@PathVariable String id, @AuthenticationPrincipal Actor actor) {
        return invoiceService.get(id, actor);
    }

    @Operation(summary = "Xem trước mã hóa đơn kế tiếp (Bán thuốc)")
    @GetMapping("/next-number")
    @PreAuthorize("hasRole('SALES')")
    public CodePreviewResponse nextNumber() {
        return checkoutService.nextNumber();
    }

    @Operation(
            summary =
                    "Lập hóa đơn: chọn lô theo hạn dùng gần nhất, server tự tính tiền, requestId chống gửi trùng (Bán thuốc)")
    @PostMapping
    @PreAuthorize("hasRole('SALES')")
    public InvoiceDetailResponse create(
            @Valid @RequestBody CreateInvoiceRequest request, @AuthenticationPrincipal Actor actor) {
        return checkoutService.create(request, actor);
    }

    @Operation(summary = "Hủy hóa đơn và hoàn kho về đúng lô cũ (Admin)")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public InvoiceDetailResponse cancel(
            @PathVariable String id,
            @Valid @RequestBody CancelInvoiceRequest request,
            @AuthenticationPrincipal Actor actor) {
        return invoiceService.cancel(id, request.reason(), actor);
    }
}
