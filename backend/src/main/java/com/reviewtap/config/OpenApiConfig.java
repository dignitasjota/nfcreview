package com.reviewtap.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("ReviewTap API")
                .version("0.1.0")
                .description("""
                        API del SaaS de dispositivos NFC/QR para reseñas de Google.
                        Autenticación mediante cookie HttpOnly emitida por POST /api/auth/login.
                        Las cifras representan interacciones (aperturas del enlace), nunca reseñas publicadas.
                        """));
    }
}
