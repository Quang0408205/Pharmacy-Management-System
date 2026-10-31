package com.pharmacy.demo.entity.embedded;

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
public class BatchAllocation {

    @Field("id_lo_thuoc")
    private String batchId;

    @Field("so_lo")
    private String batchCode;

    @Field("han_su_dung")
    private LocalDate expiryDate;

    @Field("so_luong_xuat")
    private Integer quantity;
}
