package com.pharmacy.demo.entity;

import com.pharmacy.demo.entity.embedded.StocktakeLine;
import com.pharmacy.demo.enums.StocktakeStatus;
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
@CompoundIndex(name = "trang_thai_ngay_tao", def = "{'trang_thai': 1, 'ngay_tao': -1}")
@Document(collection = "phieu_kiem_ke")
public class Stocktake {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("ma_phieu_kiem_ke")
    private String code;

    @Field("id_danh_muc")
    private String categoryId;

    @Field("nhan_pham_vi")
    private String scopeLabel;

    @Field("ghi_chu")
    private String note;

    @Field("id_nhan_vien_lap")
    private String createdById;

    @Field("ten_nhan_vien_lap")
    private String createdByName;

    @Field("chi_tiet_kiem_ke")
    private List<StocktakeLine> lines;

    @Field("trang_thai")
    private StocktakeStatus status;

    @Field("ngay_tao")
    private Instant createdAt;

    @Field("ngay_gui_duyet")
    private Instant submittedAt;

    @Field("id_nguoi_duyet")
    private String reviewedById;

    @Field("ten_nguoi_duyet")
    private String reviewedByName;

    @Field("ngay_duyet")
    private Instant reviewedAt;

    @Field("ghi_chu_duyet")
    private String reviewNote;
}
