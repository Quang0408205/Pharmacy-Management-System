package com.pharmacy.demo.entity;

import com.pharmacy.demo.entity.embedded.DataSource;
import com.pharmacy.demo.entity.embedded.UnitConversion;
import com.pharmacy.demo.enums.MedicineStatus;
import com.pharmacy.demo.enums.PrescriptionType;
import java.time.Instant;
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
@CompoundIndex(name = "danh_muc_trang_thai", def = "{'id_danh_muc': 1, 'trang_thai': 1}")
@Document(collection = "thuoc")
public class Medicine {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("ma_thuoc")
    private String code;

    @Indexed(unique = true, partialFilter = "{ 'ma_vach': { $type: 'string' } }")
    @Field("ma_vach")
    private String barcode;

    @Field("ten_thuoc")
    private String name;

    @Field("ten_hoat_chat")
    private String genericName;

    @Field("ham_luong")
    private String strength;

    @Field("dang_bao_che")
    private String dosageForm;

    @Field("id_danh_muc")
    private String categoryId;

    @Field("nha_san_xuat")
    private String manufacturer;

    @Field("nuoc_san_xuat")
    private String country;

    @Field("so_dang_ky")
    private String registrationNumber;

    @Field("don_vi_ban")
    private String unit;

    @Field("quy_cach_dong_goi")
    private String packaging;

    @Field("quy_doi_don_vi")
    private List<UnitConversion> unitConversions;

    @Field("gia_ban")
    private Long price;

    @Field("gia_nhap_tham_khao")
    private Long costPrice;

    @Field("loai_ke_don")
    private PrescriptionType prescriptionType;

    @Field("muc_ton_toi_thieu")
    private Integer minStock;

    @Field("duong_dan_anh")
    private String imageUrl;

    @Field("mo_ta")
    private String description;

    @Field("nguon_du_lieu")
    private DataSource dataSource;

    @Field("trang_thai")
    private MedicineStatus status;

    @Field("ngay_tao")
    private Instant createdAt;

    @Field("ngay_cap_nhat")
    private Instant updatedAt;
}
