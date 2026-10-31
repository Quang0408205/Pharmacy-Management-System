package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.util.PhoneMatcher;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "Tên khách hàng là bắt buộc.") @Size(max = 100, message = "Tên tối đa 100 ký tự.")
        String name,

        @NotBlank(message = "Số điện thoại là bắt buộc.")
        @Pattern(regexp = PhoneMatcher.PATTERN, message = PhoneMatcher.MESSAGE)
        String phone,

        @Email(message = "Email không hợp lệ.") @Size(max = 150, message = "Email tối đa 150 ký tự.")
        String email,

        @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự.")
        String address,

        @Size(max = 10, message = "Giới tính tối đa 10 ký tự.")
        String gender,

        @Min(value = 1900, message = "Năm sinh không hợp lệ.") @Max(value = 2100, message = "Năm sinh không hợp lệ.")
        Integer birthYear,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự.")
        String note) {}
