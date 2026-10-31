package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.enums.DiscountType;
import com.pharmacy.demo.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateInvoiceRequest(
        @NotEmpty(message = "Hóa đơn phải có ít nhất một loại thuốc.")
        @Size(max = 100, message = "Hóa đơn tối đa 100 loại thuốc.")
        List<@Valid InvoiceItemRequest> items,

        String customerId,
        DiscountType discountType,

        @PositiveOrZero(message = "Giá trị giảm giá không hợp lệ.")
        Long discountValue,

        @NotNull(message = "Phương thức thanh toán là bắt buộc.")
        PaymentMethod paymentMethod,

        @PositiveOrZero(message = "Số tiền khách trả không hợp lệ.")
        Long receivedAmount,

        @PositiveOrZero(message = "Số điểm sử dụng không hợp lệ.")
        Integer pointsUsed,

        Boolean prescriptionConfirmed,

        @Size(max = 100, message = "Mã đơn thuốc tối đa 100 ký tự.")
        String prescriptionCode,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
        String note,

        String draftId,

        @Size(max = 100, message = "Mã yêu cầu tối đa 100 ký tự.")
        String requestId) {}
