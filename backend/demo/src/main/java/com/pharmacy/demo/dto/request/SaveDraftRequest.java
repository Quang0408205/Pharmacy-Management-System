package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.enums.DiscountType;
import com.pharmacy.demo.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveDraftRequest(
        String id,
        String customerId,

        @NotEmpty(message = "Đơn nháp phải có ít nhất một loại thuốc.")
        @Size(max = 100, message = "Đơn nháp tối đa 100 loại thuốc.")
        List<@Valid InvoiceItemRequest> items,

        DiscountType discountType,

        @PositiveOrZero(message = "Giá trị giảm giá không hợp lệ.")
        Long discountValue,

        @NotNull(message = "Phương thức thanh toán là bắt buộc.")
        PaymentMethod paymentMethod,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
        String note) {}
