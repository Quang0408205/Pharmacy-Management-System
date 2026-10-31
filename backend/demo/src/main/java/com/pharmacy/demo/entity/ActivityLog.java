package com.pharmacy.demo.entity;

import com.pharmacy.demo.enums.Role;
import java.time.Instant;
import java.util.Map;
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
@Document(collection = "nhat_ky_hoat_dong")
public class ActivityLog {

    @Id
    private String id;

    @Field("id_nhan_vien")
    private String actorId;

    @Field("ten_nhan_vien")
    private String actorName;

    @Field("vai_tro_luc_thuc_hien")
    private Role role;

    @Field("hanh_dong")
    private String action;

    @Field("loai_doi_tuong")
    private String entity;

    @Field("id_doi_tuong")
    private String entityId;

    @Field("ma_doi_tuong")
    private String entityCode;

    @Field("mo_ta")
    private String description;

    @Field("du_lieu_thay_doi")
    private Map<String, Object> changes;

    @Field("dia_chi_mang")
    private String ipAddress;

    @Indexed()
    @Field("thoi_diem")
    private Instant createdAt;
}
