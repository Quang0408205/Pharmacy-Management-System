package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.InvoiceDetailResponse;
import com.pharmacy.demo.dto.response.InvoiceSummaryResponse;
import com.pharmacy.demo.enums.InvoiceStatus;
import com.pharmacy.demo.enums.PaymentMethod;
import com.pharmacy.demo.security.Actor;
import java.time.LocalDate;

public interface InvoiceService {

    PageResponse<InvoiceSummaryResponse> search(InvoiceFilter filter, PageQuery query, Actor actor);

    InvoiceDetailResponse get(String id, Actor actor);

    InvoiceDetailResponse cancel(String id, String reason, Actor actor);

    record InvoiceFilter(
            String q,
            String employeeId,
            boolean mine,
            InvoiceStatus status,
            PaymentMethod paymentMethod,
            LocalDate from,
            LocalDate to) {}
}
