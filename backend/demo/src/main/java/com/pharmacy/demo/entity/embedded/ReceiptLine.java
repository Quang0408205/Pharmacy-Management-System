package com.pharmacy.demo.entity.embedded;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptLine {

    @Field("so_dong")
    private Integer lineNo;

    @Field("id_thuoc")
    private String medicineId;

    @Field("ma_thuoc")
    private String medicineCode;

    @Field("ten_thuoc")
    private String medicineName;

    @Field("so_lo")
    private String batchCode;

    @Field("ngay_san_xuat")
    private LocalDate manufactureDate;

    @Field("han_su_dung")
    private LocalDate expiryDate;

    @Field("so_luong")
    private Integer quantity;

    @Field("don_vi")
    private String unit;

    @Field("don_gia_nhap")
    private Long purchasePrice;

    @Field("thanh_tien")
    private Long subtotal;

    @Field("id_lo_thuoc")
    private String batchId;
}
