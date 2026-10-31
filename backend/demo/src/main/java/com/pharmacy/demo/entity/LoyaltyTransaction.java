package com.pharmacy.demo.entity;

import com.pharmacy.demo.enums.LoyaltyType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "khach_hang_ngay_tao", def = "{'id_khach_hang': 1, 'ngay_tao': -1}")
@Document(collection = "lich_su_diem")
public class LoyaltyTransaction {

    @Id
    private String id;

    @Field("id_khach_hang")
    private String customerId;

    @Field("loai")
    private LoyaltyType type;

    @Field("so_diem")
    private Integer points;

    @Field("diem_sau")
    private Integer balanceAfter;

    @Field("id_hoa_don")
    private String invoiceId;

    @Field("ma_hoa_don")
    private String invoiceCode;

    @Field("ghi_chu")
    private String note;

    @Field("id_nhan_vien")
    private String actorId;

    @Field("ten_nhan_vien")
    private String actorName;

    @Field("ngay_tao")
    private Instant createdAt;
}
