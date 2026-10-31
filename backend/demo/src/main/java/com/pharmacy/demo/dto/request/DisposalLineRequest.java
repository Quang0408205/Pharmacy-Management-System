package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.enums.DisposalReason;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DisposalLineRequest(
        @NotBlank(message = "Lô thuốc là bắt buộc.") String batchId,

        @NotNull(message = "Số lượng là bắt buộc.") @Min(value = 1, message = "Số lượng phải lớn hơn 0.")
        Integer quantity,

        @NotNull(message = "Loại lý do là bắt buộc.") DisposalReason reasonType,

        @Size(max = 300, message = "Lý do chi tiết tối đa 300 ký tự.")
        String reasonDetail) {}
