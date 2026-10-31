package com.pharmacy.demo.service.impl;

import com.pharmacy.demo.dto.common.ListPager;
import com.pharmacy.demo.dto.common.PageQuery;
import com.pharmacy.demo.dto.common.PageResponse;
import com.pharmacy.demo.dto.response.ReceiptDetailResponse;
import com.pharmacy.demo.dto.response.ReceiptSummaryResponse;
import com.pharmacy.demo.entity.GoodsReceipt;
import com.pharmacy.demo.exception.NotFoundException;
import com.pharmacy.demo.mapper.ReceiptMapper;
import com.pharmacy.demo.repository.GoodsReceiptRepository;
import com.pharmacy.demo.service.ReceiptService;
import com.pharmacy.demo.util.TextSearch;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private static final Map<String, Comparator<ReceiptSummaryResponse>> SORTABLE = Map.of(
            "code", ListPager.text(ReceiptSummaryResponse::code),
            "receivingDate", ListPager.comparable(ReceiptSummaryResponse::receivingDate),
            "supplierName", ListPager.text(ReceiptSummaryResponse::supplierName),
            "totalQuantity", ListPager.comparable(ReceiptSummaryResponse::totalQuantity),
            "totalAmount", ListPager.comparable(ReceiptSummaryResponse::totalAmount),
            "status", ListPager.comparable(ReceiptSummaryResponse::status),
            "createdByName", ListPager.text(ReceiptSummaryResponse::createdByName));

    private final GoodsReceiptRepository receiptRepository;
    private final ReceiptMapper receiptMapper;

    @Override
    public PageResponse<ReceiptSummaryResponse> search(ReceiptFilter filter, PageQuery query) {
        List<ReceiptSummaryResponse> items = receiptRepository.findAll().stream()
                .filter(receipt -> filter.status() == null || receipt.getStatus() == filter.status())
                .filter(receipt ->
                        filter.supplierId() == null || filter.supplierId().equals(receipt.getSupplierId()))
                .filter(receipt -> inRange(receipt, filter.from(), filter.to()))
                .filter(receipt -> TextSearch.matchesAny(
                        filter.q(), receipt.getCode(), receipt.getSupplierName(), receipt.getCreatedByName()))
                .map(receiptMapper::toSummary)
                .toList();
        return ListPager.paginate(items, query, SORTABLE, "receivingDate,desc");
    }

    @Override
    public ReceiptDetailResponse get(String id) {
        GoodsReceipt receipt =
                receiptRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu nhập."));
        return receiptMapper.toDetail(receipt);
    }

    private static boolean inRange(GoodsReceipt receipt, LocalDate from, LocalDate to) {
        LocalDate date = receipt.getReceivingDate();
        if (date == null) {
            return from == null && to == null;
        }
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }
}
