package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.BatchResponse;
import com.pharmacy.demo.entity.StockBatch;
import com.pharmacy.demo.enums.BatchStatus;
import com.pharmacy.demo.security.Actor;
import java.util.List;

public interface BatchQueryService {

    PageResponse<BatchResponse> search(BatchFilter filter, PageQuery query, Actor actor);

    BatchResponse get(String id);

    List<BatchResponse> toResponses(List<StockBatch> batches, boolean salesView);

    record BatchFilter(
            String q,
            String medicineId,
            String supplierId,
            BatchStatus status,
            Integer expiryWithin,
            boolean hasStock) {}

    List<BatchResponse> expiringSoon(int limit);
}
