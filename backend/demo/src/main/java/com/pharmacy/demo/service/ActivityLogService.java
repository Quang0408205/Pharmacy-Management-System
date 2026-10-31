package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.ActivityResponse;
import com.pharmacy.demo.enums.Role;
import java.time.LocalDate;

public interface ActivityLogService {

    PageResponse<ActivityResponse> search(ActivityFilter filter, PageQuery query);

    record ActivityFilter(String q, Role role, LocalDate from, LocalDate to) {}
}
