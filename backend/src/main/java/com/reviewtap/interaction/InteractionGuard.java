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
 *   <li><b>Techo por IP</b>: desde una misma IP (sin tener en cuenta el User-Agent, que el cliente
 *       puede rotar) no se registran más de N interacciones (40 por defecto) en 10 minutos sumando
 *       todos los dispositivos.
 *   <li><b>Techo por dispositivo</b>: independientemente de quién lo use, un dispositivo no registra
 *       más de M interacciones por minuto (60 por defecto). Es la red de seguridad frente a
 *       cabeceras falsificadas: aunque cada petición simule un cliente distinto, la cifra no se
 *       dispara. Un mostrador real nunca se acerca a ese ritmo.
 * </ol>
 *
 * En ambos casos el visitante es redirigido igualmente; sólo se omite el registro. Al ser
 * estado en memoria, es por instancia; suficiente para un despliegue single-node.
 */
@Component
public class InteractionGuard {

    private final Cache<String, Boolean> recent;
    private final Cache<String, AtomicInteger> perClient;
    private final Cache<UUID, AtomicInteger> perDevice;
    private final int clientRateLimit;
    private final int deviceRateLimit;

    public InteractionGuard(AppProperties props) {
        AppProperties.Interactions cfg = props.interactions();
        this.recent = Caffeine.newBuilder().expireAfterWrite(cfg.dedupeWindow()).maximumSize(200_000).build();
        this.perClient = Caffeine.newBuilder().expireAfterWrite(cfg.clientRateWindow()).maximumSize(100_000).build();
        this.perDevice = Caffeine.newBuilder().expireAfterWrite(cfg.deviceRateWindow()).maximumSize(100_000).build();
        this.clientRateLimit = cfg.clientRateLimit();
        this.deviceRateLimit = cfg.deviceRateLimit();
    }

    /** @return {@code true} si la interacción debe registrarse. */
    public boolean shouldRecord(UUID deviceId, ClientKeyResolver.ClientKeys keys) {
        String dedupeKey = deviceId + ":" + keys.client();
        if (recent.asMap().putIfAbsent(dedupeKey, Boolean.TRUE) != null) {
            return false;
        }
        AtomicInteger counter = perClient.get(keys.ip(), k -> new AtomicInteger());
        if (counter.incrementAndGet() > clientRateLimit) {
            return false;
        }
        AtomicInteger deviceCounter = perDevice.get(deviceId, k -> new AtomicInteger());
        return deviceCounter.incrementAndGet() <= deviceRateLimit;
    }

    /** Vacía el estado en memoria (tests). */
    public void reset() {
        recent.invalidateAll();
        perClient.invalidateAll();
        perDevice.invalidateAll();
    }
}
