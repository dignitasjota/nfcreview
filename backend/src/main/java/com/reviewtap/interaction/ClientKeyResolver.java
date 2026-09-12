package com.reviewtap.interaction;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Deriva una clave efímera y anónima del visitante para deduplicar accesos accidentales.
 *
 * <p>clave = HMAC-SHA256(salt, ip + "|" + user-agent), truncada. El salt se genera con
 * SecureRandom al arrancar y se rota cada día UTC, así que la clave no es reversible, no se
 * persiste nunca y ni siquiera es estable entre días o reinicios. La IP sólo existe en memoria
 * durante el cálculo.
 */
@Component
public class ClientKeyResolver {

    private static final SecureRandom RANDOM = new SecureRandom();

    private record Salt(LocalDate day, byte[] value) {}

    private final AtomicReference<Salt> salt = new AtomicReference<>(newSalt(LocalDate.now(ZoneOffset.UTC)));

    public String resolve(HttpServletRequest request) {
        String ip = clientIp(request);
        String ua = request.getHeader("User-Agent");
        String material = ip + "|" + (ua == null ? "" : ua);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(currentSalt(), "HmacSHA256"));
            byte[] digest = mac.doFinal(material.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 22);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC no disponible", e);
        }
    }

    /**
     * Detrás de un reverse proxy la IP real llega en X-Forwarded-For (primer salto). Sólo se usa
     * para la clave anónima; un valor falso únicamente afecta a la deduplicación de ese cliente.
     */
    static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }

    private byte[] currentSalt() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Salt current = salt.get();
        if (!current.day().equals(today)) {
            salt.compareAndSet(current, newSalt(today));
            current = salt.get();
        }
        return current.value();
    }

    private static Salt newSalt(LocalDate day) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return new Salt(day, bytes);
    }
}
