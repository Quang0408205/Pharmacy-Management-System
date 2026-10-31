package com.pharmacy.demo.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StocktakeLine {

    @Field("id_lo_thuoc")
    private String batchId;

    @Field("so_lo")
    private String batchCode;

    @Field("id_thuoc")
    private String medicineId;

    @Field("ten_thuoc")
    private String medicineName;

    @Field("so_luong_he_thong")
    private Integer systemQty;

    @Field("so_luong_thuc_te")
    private Integer actualQty;

    @Field("chenh_lech")
    private Integer difference;

    @Field("ly_do")
    private String reason;

    @Field("phien_ban_lo")
    private Long batchVersion;
}
