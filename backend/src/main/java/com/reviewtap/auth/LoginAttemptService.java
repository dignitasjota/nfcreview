package com.reviewtap.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.reviewtap.config.AppProperties;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Bloqueo temporal de login (en memoria) con dos claves:
 * <ul>
 *   <li>{@code email + IP}: N fallos seguidos bloquean esa combinación. Un atacante que conozca el
 *       email de un cliente no puede dejarlo sin acceso desde otra red.
 *   <li>{@code IP}: un techo global (5×N) frena la enumeración de cuentas desde una misma IP.
 * </ul>
 */
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

    public boolean isBlocked(String email, String ip) {
        return count(accountKey(email, ip)) >= maxAttempts || count(ipKey(ip)) >= maxAttempts * 5;
    }

    public void recordFailure(String email, String ip) {
        failures.get(accountKey(email, ip), k -> new AtomicInteger()).incrementAndGet();
        failures.get(ipKey(ip), k -> new AtomicInteger()).incrementAndGet();
    }

    public void reset(String email, String ip) {
        failures.invalidate(accountKey(email, ip));
    }

    private int count(String key) {
        AtomicInteger c = failures.getIfPresent(key);
        return c == null ? 0 : c.get();
    }

    private static String accountKey(String email, String ip) {
        return "a:" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT)) + "@" + ip;
    }

    private static String ipKey(String ip) {
        return "ip:" + ip;
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
