package com.pharmacy.demo.entity.embedded;

import java.time.Instant;
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
public class DataSource {

    @Field("loai_nguon")
    private String sourceType;

    @Field("duong_dan_nguon")
    private String sourceUrl;

    @Field("thoi_diem_thu_thap")
    private Instant collectedAt;

    @Field("da_xac_minh")
    private boolean verified;
}
