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

    /** Claves anónimas del visitante: {@code client} incluye el User-Agent; {@code ip} sólo la IP. */
    public record ClientKeys(String client, String ip) {}

    private record Salt(LocalDate day, byte[] value) {}

    private final AtomicReference<Salt> salt = new AtomicReference<>(newSalt(LocalDate.now(ZoneOffset.UTC)));

    public ClientKeys resolve(HttpServletRequest request) {
        String ip = clientIp(request);
        String ua = request.getHeader("User-Agent");
        byte[] salt = currentSalt();
        return new ClientKeys(hmac(salt, ip + "|" + (ua == null ? "" : ua)), hmac(salt, ip));
    }

    private static String hmac(byte[] salt, String material) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt, "HmacSHA256"));
            byte[] digest = mac.doFinal(material.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 22);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC no disponible", e);
        }
    }

    /**
     * IP del cliente tal y como la resuelve Tomcat ({@code server.forward-headers-strategy=native}):
     * el RemoteIpValve recorre X-Forwarded-For desde la derecha saltando los proxies internos
     * (rangos privados y {@code TRUSTED_PROXIES}) y deja en {@code getRemoteAddr()} el primer salto
     * no confiable. NO leemos la cabecera nosotros: el cliente puede fabricar sus primeros valores y,
     * además, el valve reescribe la cabecera dejando precisamente esos saltos no confiables.
     *
     * <p>Si la cadena sólo contiene direcciones privadas, Tomcat deja la primera (fabricable). Un
     * visitante real llega siempre desde una IP pública, así que cualquier IP privada/loopback se
     * agrupa en un único cubo: no puede usarse para simular clientes distintos.
     */
    public static String clientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return isPrivate(ip) ? "private" : ip;
    }

    static boolean isPrivate(String ip) {
        if (ip == null) {
            return true;
        }
        try {
            java.net.InetAddress addr = java.net.InetAddress.getByName(ip);
            return addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()
                    || addr.isAnyLocalAddress() || isCgnatOr172(ip);
        } catch (java.net.UnknownHostException e) {
            return true;
        }
    }

    /** 172.16/12 ya lo cubre isSiteLocalAddress; se añade 100.64/10 (CGNAT interno de algunos proxies). */
    private static boolean isCgnatOr172(String ip) {
        if (!ip.startsWith("100.")) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 64 && second <= 127;
        } catch (NumberFormatException e) {
            return false;
        }
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
