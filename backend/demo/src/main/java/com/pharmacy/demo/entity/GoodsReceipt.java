package com.pharmacy.demo.entity;

import com.pharmacy.demo.entity.embedded.ReceiptLine;
import com.pharmacy.demo.enums.ReceiptStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "trang_thai_ngay_nhap", def = "{'trang_thai': 1, 'ngay_nhap': -1}")
@Document(collection = "phieu_nhap_thuoc")
public class GoodsReceipt {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("ma_phieu_nhap")
    private String code;

    @Field("id_nha_cung_cap")
    private String supplierId;

    @Field("ten_nha_cung_cap")
    private String supplierName;

    @Field("id_nhan_vien_lap")
    private String createdById;

    @Field("ten_nhan_vien_lap")
    private String createdByName;

    @Field("ngay_nhap")
    private LocalDate receivingDate;

    @Field("chi_tiet_phieu_nhap")
    private List<ReceiptLine> items;

    @Field("tong_so_luong")
    private Integer totalQuantity;

    @Field("tong_tien_nhap")
    private Long totalAmount;

    @Field("trang_thai")
    private ReceiptStatus status;

    @Field("ghi_chu")
    private String note;

    @Field("ngay_tao")
    private Instant createdAt;

    @Field("ngay_xac_nhan")
    private Instant confirmedAt;
}
