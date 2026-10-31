package com.pharmacy.demo.entity.embedded;

import com.pharmacy.demo.enums.DisposalReason;
import java.time.LocalDate;
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
public class DisposalLine {

    @Field("id_lo_thuoc")
    private String batchId;

    @Field("so_lo")
    private String batchCode;

    @Field("id_thuoc")
    private String medicineId;

    @Field("ten_thuoc")
    private String medicineName;

    @Field("han_su_dung")
    private LocalDate expiryDate;

    @Field("loai_ly_do")
    private DisposalReason reasonType;

    @Field("so_luong_xuat_huy")
    private Integer quantity;

    @Field("ly_do_chi_tiet")
    private String reasonDetail;
}
