package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.StocktakeDetailResponse;
import com.pharmacy.demo.dto.response.StocktakeSummaryResponse;
import com.pharmacy.demo.enums.StocktakeStatus;
import com.pharmacy.demo.security.Actor;

public interface StocktakeService {

    PageResponse<StocktakeSummaryResponse> search(String q, StocktakeStatus status, PageQuery query);

    StocktakeDetailResponse get(String id);

    StocktakeDetailResponse approve(String id, String note, Actor actor);

    StocktakeDetailResponse reject(String id, String note, Actor actor);
}
