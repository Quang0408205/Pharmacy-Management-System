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
public class AppearanceSetting {

    @Field("che_do")
    private String theme;

    @Field("mat_do")
    private String density;
}
