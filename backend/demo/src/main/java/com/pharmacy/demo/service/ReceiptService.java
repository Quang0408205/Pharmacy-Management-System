package com.pharmacy.demo.service;

import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.ReceiptDetailResponse;
import com.pharmacy.demo.dto.response.ReceiptSummaryResponse;
import com.pharmacy.demo.enums.ReceiptStatus;
import java.time.LocalDate;

public interface ReceiptService {

    PageResponse<ReceiptSummaryResponse> search(ReceiptFilter filter, PageQuery query);

    ReceiptDetailResponse get(String id);

    record ReceiptFilter(String q, ReceiptStatus status, String supplierId, LocalDate from, LocalDate to) {}
}
