package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.MovementResponse;
import com.pharmacy.demo.enums.MovementType;
import java.time.LocalDate;

public interface MovementService {

    PageResponse<MovementResponse> search(MovementFilter filter, PageQuery query);

    record MovementFilter(
            String q, MovementType type, String medicineId, String batchId, LocalDate from, LocalDate to) {}
}
