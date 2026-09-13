package com.reviewtap.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Configuración propia de la aplicación (prefijo {@code app.*}). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String publicBaseUrl,
        String appBaseUrl,
        Mail mail,
        Cors cors,
        Security security,
        Interactions interactions,
        Bootstrap bootstrap,
        Seed seed) {

    public record Cors(List<String> allowedOrigins) {}

    public record Mail(String from) {}

    public record Security(
            String jwtSecret,
            @DefaultValue("PT8H") Duration jwtTtl,
            @DefaultValue("rt_session") String cookieName,
            @DefaultValue("true") boolean cookieSecure,
            @DefaultValue("8") int loginMaxAttempts,
            @DefaultValue("PT15M") Duration loginLockout) {}

    public record Interactions(
            @DefaultValue("PT60S") Duration dedupeWindow,
            @DefaultValue("40") int clientRateLimit,
            @DefaultValue("PT10M") Duration clientRateWindow,
            @DefaultValue("60") int deviceRateLimit,
            @DefaultValue("PT1M") Duration deviceRateWindow) {}

    public record Bootstrap(String adminEmail, String adminPassword) {}

    public record Seed(@DefaultValue("false") boolean enabled, String password) {}

    /** URL del panel sin barra final, p. ej. {@code https://app.midominio.com}. */
    public String normalizedAppBaseUrl() {
        String base = appBaseUrl == null ? "" : appBaseUrl.trim();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    /** URL base pública sin barra final, p. ej. {@code https://r.midominio.com}. */
    public String normalizedPublicBaseUrl() {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
