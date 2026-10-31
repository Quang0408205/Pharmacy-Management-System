package com.pharmacy.demo.entity;

import com.pharmacy.demo.enums.SupplierStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "nha_cung_cap")
public class Supplier {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("ma_nha_cung_cap")
    private String code;

    @Field("ten_nha_cung_cap")
    private String name;

    @Field("nguoi_lien_he")
    private String contactName;

    @Field("so_dien_thoai")
    private String phone;

    @Field("thu_dien_tu")
    private String email;

    @Field("dia_chi")
    private String address;

    @Field("ma_so_thue")
    private String taxCode;

    @Field("dieu_kien_thanh_toan")
    private String paymentTerms;

    @Field("ghi_chu")
    private String note;

    @Field("trang_thai")
    private SupplierStatus status;

    @Field("ngay_tao")
    private Instant createdAt;

    @Field("ngay_cap_nhat")
    private Instant updatedAt;
}
