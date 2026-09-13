package com.reviewtap.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.reviewtap.common.ApiException;
import com.reviewtap.config.AppProperties;
import com.reviewtap.user.User;
import com.reviewtap.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Olvidé mi contraseña". El endpoint de solicitud responde siempre igual (no revela si el email
 * existe). El token (32 bytes aleatorios) viaja en el enlace del email; en BD sólo se guarda su
 * SHA-256, caduca en 1 h y es de un solo uso. Restablecer la contraseña revoca todas las sesiones.
 */
@Slf4j
@Service
public class PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofHours(1);
    private static final int MAX_REQUESTS_PER_HOUR = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final MailService mail;
    private final String appBaseUrl;
    private final Cache<String, AtomicInteger> requests = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1)).maximumSize(50_000).build();

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
            PasswordEncoder passwordEncoder, MailService mail, AppProperties props) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.mail = mail;
        this.appBaseUrl = props.normalizedAppBaseUrl();
    }

    /**
     * Genera y envía el token. Devuelve el token en claro sólo para los tests; el controlador lo ignora.
     * Limita a 3 solicitudes/hora por email y por IP.
     */
    @Transactional
    public Optional<String> request(String email, String clientIp) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (bump("e:" + normalized) > MAX_REQUESTS_PER_HOUR || bump("ip:" + clientIp) > MAX_REQUESTS_PER_HOUR * 5) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS",
                    "Demasiadas solicitudes. Inténtalo de nuevo más tarde");
        }
        Optional<User> found = users.findByEmailIgnoreCase(normalized).filter(User::isEnabled);
        if (found.isEmpty()) {
            log.info("Solicitud de restablecimiento para un email desconocido o deshabilitado");
            return Optional.empty();
        }
        User user = found.get();
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = HexFormat.of().formatHex(raw);
        tokens.save(new PasswordResetToken(user, sha256(token), Instant.now().plus(TOKEN_TTL)));

        String link = appBaseUrl + "/reset-password?token=" + token;
        boolean sent = mail.send(user.getEmail(), "Restablecer tu contraseña",
                "Hola " + user.getFirstName() + ",\n\n"
                + "Hemos recibido una solicitud para restablecer la contraseña de tu cuenta.\n"
                + "Abre este enlace (caduca en 1 hora):\n\n" + link + "\n\n"
                + "Si no has sido tú, ignora este mensaje: tu contraseña no cambia.\n");
        if (sent) {
            log.info("Email de restablecimiento enviado al usuario {}", user.getId());
        } else {
            log.warn("SMTP no configurado o fallo de envío: el usuario {} ({}) ha solicitado restablecer su "
                    + "contraseña. Un administrador puede hacerlo desde el panel.", user.getId(),
                    LoginAttemptService.mask(user.getEmail()));
        }
        return Optional.of(token);
    }

    @Transactional
    public void reset(String token, String newPassword) {
        Instant now = Instant.now();
        PasswordResetToken prt = tokens.findByTokenHash(sha256(token == null ? "" : token.trim()))
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN",
                        "El enlace no es válido o ha caducado. Solicita uno nuevo"));
        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.revokeSessions();
        prt.setUsedAt(now);
        log.info("Contraseña restablecida mediante enlace por el usuario {}", user.getId());
    }

    /** Limpieza diaria de tokens caducados o usados. */
    @Scheduled(cron = "0 15 4 * * *")
    @Transactional
    public void purge() {
        int removed = tokens.purge(Instant.now());
        if (removed > 0) {
            log.info("Tokens de restablecimiento purgados: {}", removed);
        }
    }

    private int bump(String key) {
        return requests.get(key, k -> new AtomicInteger()).incrementAndGet();
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
