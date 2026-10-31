package com.pharmacy.demo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SettingsRequest(
        @Valid Pharmacy pharmacy,

        @DecimalMin(value = "0.5", message = "Hệ số cảnh báo phải từ 0.5 đến 5.")
        @DecimalMax(value = "5", message = "Hệ số cảnh báo phải từ 0.5 đến 5.")
        Double lowStockMultiplier,

        @Min(value = 7, message = "Số ngày cảnh báo phải từ 7 đến 365.")
        @Max(value = 365, message = "Số ngày cảnh báo phải từ 7 đến 365.")
        Integer expiryWarningDays,

        List<Integer> expiryTiers,
        @Valid Appearance appearance,
        Notifications notifications,
        @Valid Loyalty loyalty) {

    public record Pharmacy(
            @Pattern(regexp = "(?s).*\\S.*", message = "Tên nhà thuốc là bắt buộc.")
            @Size(max = 200, message = "Tên nhà thuốc tối đa 200 ký tự.")
            String name,

            @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự.")
            String address,

            @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự.")
            String phone,

            @Size(max = 150, message = "Email tối đa 150 ký tự.")
            String email,

            @Size(max = 30, message = "Mã số thuế tối đa 30 ký tự.")
            String taxCode,

            @Size(max = 50, message = "Số giấy phép tối đa 50 ký tự.")
            String license) {}

    public record Appearance(
            @Pattern(regexp = "light|dark", message = "Chủ đề không hợp lệ.")
            String theme,

            @Pattern(regexp = "comfortable|compact", message = "Mật độ bảng không hợp lệ.")
            String density) {}

    public record Notifications(Boolean lowStock, Boolean expiring, Boolean newReceipt, Boolean dailyReport) {}

    public record Loyalty(
            Boolean enabled,

            @Min(value = 1000, message = "Số tiền mỗi điểm tối thiểu 1.000đ.")
            @Max(value = 10000000, message = "Số tiền mỗi điểm tối đa 10.000.000đ.")
            Long earnAmountPerPoint,

            @Min(value = 100, message = "Giá trị mỗi điểm tối thiểu 100đ.")
            @Max(value = 1000000, message = "Giá trị mỗi điểm tối đa 1.000.000đ.")
            Long pointValue,

            @Min(value = 1, message = "Tỷ lệ đổi điểm tối đa từ 1 đến 100%.")
            @Max(value = 100, message = "Tỷ lệ đổi điểm tối đa từ 1 đến 100%.")
            Integer maxRedeemPercent) {}
}
