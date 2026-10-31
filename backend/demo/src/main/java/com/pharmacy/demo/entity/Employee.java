package com.pharmacy.demo.entity;

import com.pharmacy.demo.enums.EmployeeStatus;
import com.pharmacy.demo.enums.Role;
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
@Document(collection = "nhan_vien")
public class Employee {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("ma_nhan_vien")
    private String code;

    @Field("ten_dang_nhap")
    private String username;

    @Indexed(unique = true)
    @Field("ten_dang_nhap_chuan_hoa")
    private String usernameNormalized;

    @Field("ho_ten")
    private String fullName;

    @Field("thu_dien_tu")
    private String email;

    @Indexed(unique = true)
    @Field("thu_dien_tu_chuan_hoa")
    private String emailNormalized;

    @Field("so_dien_thoai")
    private String phone;

    @Field("mat_khau_bam")
    private String passwordHash;

    @Field("vai_tro")
    private Role role;

    @Field("trang_thai")
    private EmployeeStatus status;

    @Field("phien_ban_bao_mat")
    private long securityVersion;

    @Field("so_lan_dang_nhap_sai")
    private int failedLoginCount;

    @Field("khoa_den")
    private Instant lockedUntil;

    @Field("id_nguoi_tao")
    private String createdById;

    @Field("lan_dang_nhap_cuoi")
    private Instant lastLoginAt;

    @Field("ngay_tao")
    private Instant createdAt;

    @Field("ngay_cap_nhat")
    private Instant updatedAt;
}
