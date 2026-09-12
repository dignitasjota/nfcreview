package com.reviewtap.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.reviewtap.config.AppProperties;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Bloqueo temporal por cuenta tras N intentos fallidos consecutivos (en memoria). */
@Component
public class LoginAttemptService {

    private final Cache<String, AtomicInteger> failures;
    private final int maxAttempts;

    public LoginAttemptService(AppProperties props) {
        this.maxAttempts = props.security().loginMaxAttempts();
        this.failures = Caffeine.newBuilder()
                .expireAfterWrite(props.security().loginLockout())
                .maximumSize(50_000)
                .build();
    }

    public boolean isBlocked(String email) {
        AtomicInteger count = failures.getIfPresent(key(email));
        return count != null && count.get() >= maxAttempts;
    }

    public void recordFailure(String email) {
        failures.get(key(email), k -> new AtomicInteger()).incrementAndGet();
    }

    public void reset(String email) {
        failures.invalidate(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** {@code pepe@dominio.com} → {@code p***@dominio.com}, para logs sin datos personales completos. */
    public static String mask(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }
}
