package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.ReceiptDetailResponse;
import com.pharmacy.demo.dto.response.ReceiptSummaryResponse;
import com.pharmacy.demo.entity.GoodsReceipt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ReceiptMapper {

    @Mapping(target = "itemCount", expression = "java(receipt.getItems() == null ? 0 : receipt.getItems().size())")
    ReceiptSummaryResponse toSummary(GoodsReceipt receipt);

    ReceiptDetailResponse toDetail(GoodsReceipt receipt);
}
