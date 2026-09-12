package com.reviewtap.interaction;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.reviewtap.config.AppProperties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * Protección básica contra estadísticas infladas. Dos reglas en memoria (Caffeine):
 *
 * <ol>
 *   <li><b>Deduplicación</b>: el mismo cliente anónimo sobre el mismo dispositivo sólo cuenta una
 *       vez por ventana (60 s por defecto). Acercar el móvil tres veces seguidas = 1 interacción.
 *   <li><b>Techo por cliente</b>: un mismo cliente anónimo no puede generar más de N interacciones
 *       (40 por defecto) en 10 minutos sumando todos los dispositivos.
 * </ol>
 *
 * En ambos casos el visitante es redirigido igualmente; sólo se omite el registro. Al ser
 * estado en memoria, es por instancia; suficiente para un despliegue single-node.
 */
@Component
public class InteractionGuard {

    private final Cache<String, Boolean> recent;
    private final Cache<String, AtomicInteger> perClient;
    private final int clientRateLimit;

    public InteractionGuard(AppProperties props) {
        AppProperties.Interactions cfg = props.interactions();
        this.recent = Caffeine.newBuilder().expireAfterWrite(cfg.dedupeWindow()).maximumSize(200_000).build();
        this.perClient = Caffeine.newBuilder().expireAfterWrite(cfg.clientRateWindow()).maximumSize(100_000).build();
        this.clientRateLimit = cfg.clientRateLimit();
    }

    /** @return {@code true} si la interacción debe registrarse. */
    public boolean shouldRecord(UUID deviceId, String clientKey) {
        String dedupeKey = deviceId + ":" + clientKey;
        if (recent.asMap().putIfAbsent(dedupeKey, Boolean.TRUE) != null) {
            return false;
        }
        AtomicInteger counter = perClient.get(clientKey, k -> new AtomicInteger());
        return counter.incrementAndGet() <= clientRateLimit;
    }

    /** Vacía el estado en memoria (tests). */
    public void reset() {
        recent.invalidateAll();
        perClient.invalidateAll();
    }
}
