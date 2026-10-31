package com.pharmacy.demo.entity;

import com.pharmacy.demo.entity.embedded.DisposalLine;
import com.pharmacy.demo.enums.DisposalStatus;
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
@Document(collection = "phieu_xuat_huy")
public class Disposal {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("ma_phieu_xuat_huy")
    private String code;

    @Field("id_nhan_vien_de_xuat")
    private String createdById;

    @Field("ten_nhan_vien_de_xuat")
    private String createdByName;

    @Field("chi_tiet_xuat_huy")
    private List<DisposalLine> lines;

    @Field("ly_do")
    private String reason;

    @Field("ghi_chu")
    private String note;

    @Field("trang_thai")
    private DisposalStatus status;

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

    @Field("id_nguoi_thuc_hien")
    private String executedById;

    @Field("ten_nguoi_thuc_hien")
    private String executedByName;

    @Field("ngay_thuc_hien")
    private Instant executedAt;
}
