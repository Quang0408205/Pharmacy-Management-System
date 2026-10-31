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
public class PharmacyInfo {

    @Field("ten_nha_thuoc")
    private String name;

    @Field("dia_chi")
    private String address;

    @Field("so_dien_thoai")
    private String phone;

    @Field("thu_dien_tu")
    private String email;

    @Field("ma_so_thue")
    private String taxCode;

    @Field("so_giay_phep")
    private String license;
}
