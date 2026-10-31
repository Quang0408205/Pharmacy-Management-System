package com.pharmacy.demo.entity;

import com.pharmacy.demo.enums.MovementType;
import com.pharmacy.demo.enums.ReferenceType;
import java.time.Instant;
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
@CompoundIndex(name = "lo_thoi_diem", def = "{'id_lo_thuoc': 1, 'thoi_diem': -1}")
@Document(collection = "bien_dong_kho")
public class StockMovement {

    @Id
    private String id;

    @Field("id_thuoc")
    private String medicineId;

    @Field("ma_thuoc")
    private String medicineCode;

    @Field("ten_thuoc")
    private String medicineName;

    @Field("id_lo_thuoc")
    private String batchId;

    @Field("so_lo")
    private String batchCode;

    @Field("loai_bien_dong")
    private MovementType type;

    @Field("so_luong_thay_doi")
    private Integer quantity;

    @Field("so_luong_sau")
    private Integer balanceAfter;

    @Field("loai_chung_tu")
    private ReferenceType referenceType;

    @Field("id_chung_tu")
    private String referenceId;

    @Field("ma_chung_tu")
    private String referenceCode;

    @Field("id_nhan_vien_thuc_hien")
    private String performedById;

    @Field("ten_nhan_vien")
    private String performedByName;

    @Indexed()
    @Field("thoi_diem")
    private Instant createdAt;

    @Field("ghi_chu")
    private String note;
}
