package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.request.StocktakeCreateRequest;
import com.pharmacy.demo.dto.request.StocktakeSaveRequest;
import com.pharmacy.demo.dto.response.StocktakeDetailResponse;
import com.pharmacy.demo.security.Actor;

public interface StocktakeCommandService {

    StocktakeDetailResponse create(StocktakeCreateRequest request, Actor actor);

    StocktakeDetailResponse save(String id, StocktakeSaveRequest request, Actor actor);

    StocktakeDetailResponse submit(String id, StocktakeSaveRequest request, Actor actor);

    void delete(String id, Actor actor);
}
