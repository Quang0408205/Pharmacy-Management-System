package com.pharmacy.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record StocktakeSaveRequest(
        @Size(max = 200, message = "Phiếu kiểm kê tối đa 200 dòng.")
        List<@Valid StocktakeLineRequest> lines,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
        String note) {}
