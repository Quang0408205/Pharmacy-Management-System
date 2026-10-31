package com.pharmacy.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DisposalRequest(
        @NotBlank(message = "Lý do xuất hủy là bắt buộc.") @Size(max = 300, message = "Lý do tối đa 300 ký tự.")
        String reason,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
        String note,

        @NotEmpty(message = "Phiếu xuất hủy phải có ít nhất một lô.")
        @Size(max = 100, message = "Phiếu xuất hủy tối đa 100 lô.")
        List<@Valid DisposalLineRequest> lines) {}
