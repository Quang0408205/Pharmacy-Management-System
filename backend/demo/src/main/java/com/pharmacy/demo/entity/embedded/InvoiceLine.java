package com.pharmacy.demo.entity.embedded;

import com.pharmacy.demo.enums.PrescriptionType;
import java.util.List;
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
public class InvoiceLine {

    @Field("id_thuoc")
    private String medicineId;

    @Field("ma_thuoc")
    private String medicineCode;

    @Field("ten_thuoc")
    private String medicineName;

    @Field("don_vi")
    private String unit;

    @Field("loai_ke_don")
    private PrescriptionType prescriptionType;

    @Field("so_luong")
    private Integer quantity;

    @Field("don_gia")
    private Long unitPrice;

    @Field("thanh_tien")
    private Long subtotal;

    @Field("phan_bo_lo")
    private List<BatchAllocation> allocations;
}
