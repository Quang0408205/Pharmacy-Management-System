package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.request.CreateInvoiceRequest;
import com.pharmacy.demo.dto.response.CodePreviewResponse;
import com.pharmacy.demo.dto.response.InvoiceDetailResponse;
import com.pharmacy.demo.security.Actor;

public interface CheckoutService {

    CodePreviewResponse nextNumber();

    InvoiceDetailResponse create(CreateInvoiceRequest request, Actor actor);
}
