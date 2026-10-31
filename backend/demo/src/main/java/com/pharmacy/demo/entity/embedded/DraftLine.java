package com.pharmacy.demo.entity.embedded;

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
public class DraftLine {

    @Field("id_thuoc")
    private String medicineId;

    @Field("ten_thuoc")
    private String medicineName;

    @Field("don_vi")
    private String unit;

    @Field("so_luong")
    private Integer quantity;
}
