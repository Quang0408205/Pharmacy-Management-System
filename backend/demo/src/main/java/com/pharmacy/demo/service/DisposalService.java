package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.DisposalDetailResponse;
import com.pharmacy.demo.dto.response.DisposalSummaryResponse;
import com.pharmacy.demo.enums.DisposalStatus;
import com.pharmacy.demo.security.Actor;

public interface DisposalService {

    PageResponse<DisposalSummaryResponse> search(String q, DisposalStatus status, PageQuery query);

    DisposalDetailResponse get(String id);

    DisposalDetailResponse approve(String id, String note, Actor actor);

    DisposalDetailResponse reject(String id, String note, Actor actor);
}
