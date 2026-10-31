package com.pharmacy.demo.entity;

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
@Document(collection = "khach_hang")
public class Customer {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("ma_khach_hang")
    private String code;

    @Field("ho_ten")
    private String name;

    @Indexed()
    @Field("so_dien_thoai")
    private String phone;

    @Field("thu_dien_tu")
    private String email;

    @Field("dia_chi")
    private String address;

    @Field("gioi_tinh")
    private String gender;

    @Field("nam_sinh")
    private Integer birthYear;

    @Field("ghi_chu")
    private String note;

    @Field("diem_tich_luy")
    private int loyaltyPoints;

    @Field("ngay_tao")
    private Instant createdAt;

    @Field("ngay_cap_nhat")
    private Instant updatedAt;
}
