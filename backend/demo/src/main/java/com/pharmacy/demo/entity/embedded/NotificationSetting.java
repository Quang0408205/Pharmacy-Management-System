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
public class NotificationSetting {

    @Field("ton_thap")
    private boolean lowStock;

    @Field("sap_het_han")
    private boolean expiring;

    @Field("phieu_nhap_moi")
    private boolean newReceipt;

    @Field("bao_cao_ngay")
    private boolean dailyReport;
}
