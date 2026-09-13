package com.reviewtap.auth;

import com.reviewtap.config.AppProperties;
import com.reviewtap.user.User;
import com.reviewtap.user.UserRole;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/** Emisión y validación de JWT HS256 firmados con {@code app.security.jwt-secret}. */
@Slf4j
@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final AppProperties.Security security;

    public JwtService(AppProperties props) {
        this.security = props.security();
        String secret = security.jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 32 bytes. Genera uno con: openssl rand -base64 48");
        }
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public String issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(security.jwtTtl()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("ver", user.getTokenVersion())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Optional<AuthPrincipal> parse(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            UUID userId = UUID.fromString(jwt.getSubject());
            UserRole role = UserRole.valueOf(jwt.getClaimAsString("role"));
            Long version = jwt.getClaim("ver");
            return Optional.of(new AuthPrincipal(userId, jwt.getClaimAsString("email"), role,
                    version == null ? -1 : version.intValue()));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            log.debug("JWT rechazado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public long ttlSeconds() {
        return security.jwtTtl().toSeconds();
    }
}
