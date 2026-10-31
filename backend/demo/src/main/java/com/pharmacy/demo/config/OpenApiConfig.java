package com.pharmacy.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String COOKIE_SCHEME = "cookieAuth";

    @Bean
    OpenAPI pharmacyOpenApi(AppProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title("Pharmacy Management API")
                        .version("v1")
                        .description("API quản lý nhà thuốc. Đăng nhập tại /api/v1/auth/login để nhận cookie phiên."))
                .components(new Components()
                        .addSecuritySchemes(
                                COOKIE_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name(properties.security().cookieName())))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_SCHEME));
    }
}
