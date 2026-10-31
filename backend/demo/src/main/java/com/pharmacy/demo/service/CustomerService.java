package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.CustomerRequest;
import com.pharmacy.demo.dto.response.CustomerDetailResponse;
import com.pharmacy.demo.dto.response.CustomerResponse;
import com.pharmacy.demo.security.Actor;

public interface CustomerService {

    PageResponse<CustomerResponse> search(String q, PageQuery query);

    CustomerDetailResponse get(String id);

    CustomerResponse findByPhone(String phone);

    CustomerResponse create(CustomerRequest request, Actor actor);

    CustomerResponse update(String id, CustomerRequest request, Actor actor);
}
