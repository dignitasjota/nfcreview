package com.reviewtap.interaction;

import com.reviewtap.config.AsyncConfig;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persiste interacciones fuera del hilo de la petición. */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionRecorder {

    private final InteractionRepository repository;

    @Async(AsyncConfig.INTERACTION_EXECUTOR)
    @Transactional
    public void record(UUID deviceId, InteractionType type, UserAgentCategory uaCategory, String referer) {
        try {
            repository.save(new Interaction(deviceId, Instant.now(), type, uaCategory, referer));
        } catch (RuntimeException e) {
            log.error("No se pudo registrar la interacción del dispositivo {}: {}", deviceId, e.getMessage());
        }
    }

    /** Del Referer sólo se conserva el origen (esquema + host): nunca rutas ni query strings. */
    public static String refererOrigin(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            String origin = uri.getScheme() + "://" + uri.getHost();
            return origin.length() > 255 ? null : origin;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
