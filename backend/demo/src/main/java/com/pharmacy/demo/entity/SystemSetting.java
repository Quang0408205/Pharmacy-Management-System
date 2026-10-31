package com.pharmacy.demo.entity;

import com.pharmacy.demo.entity.embedded.AppearanceSetting;
import com.pharmacy.demo.entity.embedded.LoyaltySetting;
import com.pharmacy.demo.entity.embedded.NotificationSetting;
import com.pharmacy.demo.entity.embedded.PharmacyInfo;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cau_hinh_he_thong")
public class SystemSetting {

    public static final String DEFAULT_ID = "mac_dinh";

    @Id
    private String id;

    @Field("thong_tin_nha_thuoc")
    private PharmacyInfo pharmacy;

    @Field("so_ngay_canh_bao_het_han")
    private Integer expiryWarningDays;

    @Field("nguong_canh_bao_han")
    private List<Integer> expiryTiers;

    @Field("he_so_canh_bao_ton_thap")
    private Double lowStockMultiplier;

    @Field("giao_dien")
    private AppearanceSetting appearance;

    @Field("thong_bao")
    private NotificationSetting notifications;

    @Field("tich_diem")
    private LoyaltySetting loyalty;

    @Field("don_vi_tien_te")
    private String currency;

    @Field("mui_gio")
    private String timezone;

    @Field("id_nguoi_cap_nhat")
    private String updatedById;

    @Field("ngay_cap_nhat")
    private Instant updatedAt;
}
