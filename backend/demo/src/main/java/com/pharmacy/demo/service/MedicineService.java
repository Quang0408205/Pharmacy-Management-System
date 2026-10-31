package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.MedicineResponse;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.PrescriptionType;
import com.pharmacy.demo.enums.StockStatus;
import com.pharmacy.demo.security.Actor;
import java.util.List;

public interface MedicineService {

    PageResponse<MedicineResponse> search(MedicineFilter filter, PageQuery pageQuery, Actor actor);

    MedicineResponse get(String id, Actor actor);

    record MedicineFilter(
            String q,
            String categoryId,
            MedicineStatus status,
            StockStatus stockStatus,
            PrescriptionType prescriptionType,
            boolean sellable,
            boolean inStock) {}

    List<MedicineResponse> lowStock(int limit);

    List<String> manufacturers(String q, int limit);
}
