package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.util.PhoneMatcher;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank(message = "Tên nhà cung cấp là bắt buộc.") @Size(max = 200, message = "Tên tối đa 200 ký tự.")
        String name,

        @Size(max = 100, message = "Người liên hệ tối đa 100 ký tự.")
        String contactName,

        @NotBlank(message = "Số điện thoại là bắt buộc.")
        @Pattern(regexp = PhoneMatcher.PATTERN, message = PhoneMatcher.MESSAGE)
        String phone,

        @Email(message = "Email không hợp lệ.") @Size(max = 150, message = "Email tối đa 150 ký tự.")
        String email,

        @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự.")
        String address,

        @Size(max = 20, message = "Mã số thuế tối đa 20 ký tự.")
        String taxCode,

        @Size(max = 200, message = "Điều kiện thanh toán tối đa 200 ký tự.")
        String paymentTerms,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
        String note) {}
