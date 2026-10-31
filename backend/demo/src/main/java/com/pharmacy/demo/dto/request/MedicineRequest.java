package com.pharmacy.demo.dto.request;

import com.pharmacy.demo.enums.PrescriptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MedicineRequest(
        @NotBlank(message = "Mã thuốc là bắt buộc.")
        @Size(max = 30, message = "Mã thuốc tối đa 30 ký tự.")
        @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "Mã thuốc chỉ gồm chữ, số, dấu chấm, gạch dưới, gạch ngang.")
        String code,

        @Size(max = 50, message = "Mã vạch tối đa 50 ký tự.")
        String barcode,

        @NotBlank(message = "Tên thuốc là bắt buộc.") @Size(max = 200, message = "Tên thuốc tối đa 200 ký tự.")
        String name,

        @Size(max = 300, message = "Hoạt chất tối đa 300 ký tự.")
        String genericName,

        @Size(max = 200, message = "Hàm lượng tối đa 200 ký tự.")
        String strength,

        @Size(max = 100, message = "Dạng bào chế tối đa 100 ký tự.")
        String dosageForm,

        @NotBlank(message = "Đơn vị tính là bắt buộc.") @Size(max = 30, message = "Đơn vị tính tối đa 30 ký tự.")
        String unit,

        @Size(max = 200, message = "Quy cách đóng gói tối đa 200 ký tự.")
        String packaging,

        @NotBlank(message = "Nhà sản xuất là bắt buộc.") @Size(max = 200, message = "Nhà sản xuất tối đa 200 ký tự.")
        String manufacturer,

        @Size(max = 100, message = "Nước sản xuất tối đa 100 ký tự.")
        String country,

        @Size(max = 50, message = "Số đăng ký tối đa 50 ký tự.")
        String registrationNumber,

        @NotBlank(message = "Danh mục là bắt buộc.") String categoryId,

        @NotNull(message = "Giá bán là bắt buộc.") @Positive(message = "Giá bán phải lớn hơn 0.")
        Long price,

        @PositiveOrZero(message = "Giá nhập không được âm.") Long costPrice,
        PrescriptionType prescriptionType,

        @PositiveOrZero(message = "Mức tồn tối thiểu không được âm.")
        Integer minStock,

        @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự.")
        String description) {}
