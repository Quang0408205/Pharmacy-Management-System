package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.request.MedicineRequest;
import com.pharmacy.demo.dto.response.MedicineResponse;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.security.Actor;

public interface MedicineCommandService {

    MedicineResponse create(MedicineRequest request, Actor actor);

    MedicineResponse update(String id, MedicineRequest request, Actor actor);

    MedicineResponse changeStatus(String id, MedicineStatus status, Actor actor);

    String nextCode();
}
