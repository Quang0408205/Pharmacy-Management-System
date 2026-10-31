package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.SupplierRequest;
import com.pharmacy.demo.dto.response.SupplierDetailResponse;
import com.pharmacy.demo.dto.response.SupplierResponse;
import com.pharmacy.demo.enums.SupplierStatus;
import com.pharmacy.demo.security.Actor;

public interface SupplierService {

    PageResponse<SupplierResponse> search(String q, SupplierStatus status, PageQuery query);

    SupplierDetailResponse get(String id);

    SupplierResponse create(SupplierRequest request, Actor actor);

    SupplierResponse update(String id, SupplierRequest request, Actor actor);

    SupplierResponse changeStatus(String id, SupplierStatus status, Actor actor);
}
