package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.request.ReceiptRequest;
import com.pharmacy.demo.dto.response.ReceiptDetailResponse;
import com.pharmacy.demo.security.Actor;

public interface ReceiptCommandService {

    String nextCode();

    ReceiptDetailResponse create(ReceiptRequest request, Actor actor);

    ReceiptDetailResponse update(String id, ReceiptRequest request, Actor actor);

    void delete(String id, Actor actor);
}
