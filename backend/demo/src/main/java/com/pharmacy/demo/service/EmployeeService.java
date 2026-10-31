package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.request.CreateEmployeeRequest;
import com.pharmacy.demo.dto.response.EmployeeCreatedResponse;
import com.pharmacy.demo.dto.response.EmployeeDetailResponse;
import com.pharmacy.demo.dto.response.EmployeeResponse;
import com.pharmacy.demo.dto.response.ResetLinkResponse;
import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.Role;
import com.pharmacy.demo.security.Actor;

public interface EmployeeService {

    PageResponse<EmployeeResponse> search(EmployeeFilter filter, PageQuery query);

    EmployeeDetailResponse get(String id);

    EmployeeCreatedResponse create(CreateEmployeeRequest request, Actor actor);

    EmployeeResponse changeRole(String id, Role role, Actor actor);

    EmployeeResponse changeStatus(String id, EmployeeStatus status, Actor actor);

    ResetLinkResponse issueResetLink(String id, Actor actor);

    EmployeeResponse unlock(String id, Actor actor);

    record EmployeeFilter(String q, Role role, EmployeeStatus status) {}
}
