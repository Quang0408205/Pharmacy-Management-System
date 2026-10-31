package com.pharmacy.demo.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bo_dem_ma")
public class CodeCounter {

    @Id
    private String id;

    @Field("gia_tri")
    private long value;

    @Field("ngay_cap_nhat")
    private Instant updatedAt;
}
