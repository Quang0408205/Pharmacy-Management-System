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
public class LoyaltySetting {

    public static final long DEFAULT_EARN_AMOUNT = 10_000;
    public static final long DEFAULT_POINT_VALUE = 1_000;
    public static final int DEFAULT_MAX_REDEEM_PERCENT = 50;

    @Field("bat")
    private boolean enabled;

    @Field("so_tien_moi_diem")
    private long earnAmountPerPoint;

    @Field("gia_tri_moi_diem")
    private long pointValue;

    @Field("ty_le_doi_toi_da")
    private int maxRedeemPercent;

    public static LoyaltySetting defaults() {
        return LoyaltySetting.builder()
                .enabled(true)
                .earnAmountPerPoint(DEFAULT_EARN_AMOUNT)
                .pointValue(DEFAULT_POINT_VALUE)
                .maxRedeemPercent(DEFAULT_MAX_REDEEM_PERCENT)
                .build();
    }
}
