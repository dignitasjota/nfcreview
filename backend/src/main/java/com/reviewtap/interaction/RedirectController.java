package com.reviewtap.interaction;

import com.reviewtap.device.DeviceRedirectTarget;
import com.reviewtap.device.DeviceRepository;
import com.reviewtap.device.PublicCodeGenerator;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público y crítico: {@code GET /d/{code}?src=nfc|qr}.
 *
 * <p>Una consulta (device ⋈ business por public_code, índice único), registro asíncrono de la
 * interacción y 302 inmediato. La URL de destino procede exclusivamente de la base de datos.
 */
@Slf4j
@Hidden
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final DeviceRepository deviceRepository;
    private final InteractionGuard guard;
    private final InteractionRecorder recorder;
    private final ClientKeyResolver clientKeyResolver;

    @GetMapping("/d/{code}")
    public ResponseEntity<?> redirect(@PathVariable String code, @RequestParam(required = false) String src,
            HttpServletRequest request) {

        Optional<DeviceRedirectTarget> found =
                PublicCodeGenerator.isPlausible(code) ? deviceRepository.findRedirectTarget(code) : Optional.empty();

        if (found.isEmpty()) {
            log.debug("Redirect: código desconocido");
            return unavailable(HttpStatus.NOT_FOUND);
        }
        DeviceRedirectTarget target = found.get();
        if (!target.deviceActive() || !target.businessActive()) {
            log.info("Redirect rechazado: dispositivo {} inactivo o negocio inactivo", target.deviceId());
            return unavailable(HttpStatus.GONE);
        }
        if (target.targetUrl() == null || target.targetUrl().isBlank()) {
            log.warn("Redirect rechazado: dispositivo {} sin URL de destino configurada", target.deviceId());
            return unavailable(HttpStatus.NOT_FOUND);
        }

        // Bots, previsualizaciones de enlaces (WhatsApp, iMessage…) y HEAD reciben el redirect
        // pero no cuentan: no son personas delante del dispositivo.
        UserAgentCategory category = UserAgentCategory.classify(request.getHeader("User-Agent"));
        boolean human = category != UserAgentCategory.BOT && !"HEAD".equalsIgnoreCase(request.getMethod());
        if (human && guard.shouldRecord(target.deviceId(), clientKeyResolver.resolve(request))) {
            recorder.record(target.deviceId(), InteractionType.fromSource(src), category,
                    InteractionRecorder.refererOrigin(request.getHeader("Referer")));
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target.targetUrl()))
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .build();
    }

    private ResponseEntity<String> unavailable(HttpStatus status) {
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noStore())
                .body(RedirectErrorPage.HTML);
    }
}
