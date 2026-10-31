package com.pharmacy.demo.entity;

import com.pharmacy.demo.enums.CategoryStatus;
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
@Document(collection = "danh_muc_thuoc")
public class Category {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("ma_danh_muc")
    private String code;

    @Field("ten_danh_muc")
    private String name;

    @Field("mo_ta")
    private String description;

    @Field("mau_hien_thi")
    private String color;

    @Field("trang_thai")
    private CategoryStatus status;

    @Field("ngay_tao")
    private Instant createdAt;

    @Field("ngay_cap_nhat")
    private Instant updatedAt;
}
