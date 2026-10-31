package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.request.DisposalRequest;
import com.pharmacy.demo.dto.response.DisposalDetailResponse;
import com.pharmacy.demo.security.Actor;

public interface DisposalCommandService {

    DisposalDetailResponse create(DisposalRequest request, Actor actor);

    DisposalDetailResponse update(String id, DisposalRequest request, Actor actor);

    void delete(String id, Actor actor);

    DisposalDetailResponse submit(String id, Actor actor);

    DisposalDetailResponse execute(String id, Actor actor);
}
