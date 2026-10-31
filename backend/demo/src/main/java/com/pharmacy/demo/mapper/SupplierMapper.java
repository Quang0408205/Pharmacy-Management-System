package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.ReceiptSummaryResponse;
import com.pharmacy.demo.dto.response.SupplierDetailResponse;
import com.pharmacy.demo.dto.response.SupplierResponse;
import com.pharmacy.demo.entity.Supplier;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
public interface SupplierMapper {

    SupplierResponse toResponse(Supplier supplier, long receiptCount);

    SupplierDetailResponse toDetail(
            Supplier supplier, long receiptCount, long totalAmount, List<ReceiptSummaryResponse> receipts);
}
