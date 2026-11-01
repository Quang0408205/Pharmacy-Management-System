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
    public static final long DEFAULT_SILVER_THRESHOLD = 2_000_000;
    public static final long DEFAULT_GOLD_THRESHOLD = 5_000_000;
    public static final long DEFAULT_DIAMOND_THRESHOLD = 10_000_000;
    public static final int DEFAULT_SILVER_RATE = 120;
    public static final int DEFAULT_GOLD_RATE = 150;
    public static final int DEFAULT_DIAMOND_RATE = 200;

    @Field("bat")
    private boolean enabled;

    @Field("so_tien_moi_diem")
    private long earnAmountPerPoint;

    @Field("gia_tri_moi_diem")
    private long pointValue;

    @Field("ty_le_doi_toi_da")
    private int maxRedeemPercent;

    @Field("nguong_bac")
    private long silverThreshold;

    @Field("nguong_vang")
    private long goldThreshold;

    @Field("nguong_kim_cuong")
    private long diamondThreshold;

    @Field("he_so_bac")
    private int silverRate;

    @Field("he_so_vang")
    private int goldRate;

    @Field("he_so_kim_cuong")
    private int diamondRate;

    public boolean fillMissingTiers() {
        if (silverThreshold > 0 && goldThreshold > 0 && diamondThreshold > 0) {
            return false;
        }
        silverThreshold = DEFAULT_SILVER_THRESHOLD;
        goldThreshold = DEFAULT_GOLD_THRESHOLD;
        diamondThreshold = DEFAULT_DIAMOND_THRESHOLD;
        silverRate = DEFAULT_SILVER_RATE;
        goldRate = DEFAULT_GOLD_RATE;
        diamondRate = DEFAULT_DIAMOND_RATE;
        return true;
    }

    public static LoyaltySetting defaults() {
        return LoyaltySetting.builder()
                .enabled(true)
                .earnAmountPerPoint(DEFAULT_EARN_AMOUNT)
                .pointValue(DEFAULT_POINT_VALUE)
                .maxRedeemPercent(DEFAULT_MAX_REDEEM_PERCENT)
                .silverThreshold(DEFAULT_SILVER_THRESHOLD)
                .goldThreshold(DEFAULT_GOLD_THRESHOLD)
                .diamondThreshold(DEFAULT_DIAMOND_THRESHOLD)
                .silverRate(DEFAULT_SILVER_RATE)
                .goldRate(DEFAULT_GOLD_RATE)
                .diamondRate(DEFAULT_DIAMOND_RATE)
                .build();
    }
}
