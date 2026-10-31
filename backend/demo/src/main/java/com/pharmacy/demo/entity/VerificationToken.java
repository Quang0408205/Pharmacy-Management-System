package com.pharmacy.demo.entity;

import com.pharmacy.demo.enums.TokenType;
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
@Document(collection = "ma_xac_thuc")
public class VerificationToken {

    @Id
    private String id;

    @Field("id_nhan_vien")
    private String employeeId;

    @Field("loai_ma")
    private TokenType type;

    @Indexed(unique = true)
    @Field("gia_tri_bam")
    private String tokenHash;

    @Field("ngay_tao")
    private Instant createdAt;

    @Indexed(expireAfterSeconds = 0)
    @Field("ngay_het_han")
    private Instant expiresAt;

    @Field("ngay_su_dung")
    private Instant usedAt;

    @Field("id_nguoi_tao")
    private String createdById;
}
