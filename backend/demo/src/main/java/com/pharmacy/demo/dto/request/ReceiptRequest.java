package com.pharmacy.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record ReceiptRequest(
        @NotBlank(message = "Vui lòng chọn nhà cung cấp.") String supplierId,
        LocalDate receivingDate,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
        String note,

        Boolean confirm,

        @NotEmpty(message = "Phiếu nhập chưa có dòng thuốc nào.")
        @Size(max = 200, message = "Phiếu nhập tối đa 200 dòng.")
        List<ReceiptItemRequest> items) {}
