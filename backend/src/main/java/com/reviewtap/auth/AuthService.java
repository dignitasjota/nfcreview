package com.reviewtap.auth;

import com.reviewtap.auth.AuthDtos.BusinessMembership;
import com.reviewtap.auth.AuthDtos.MeResponse;
import com.reviewtap.business.BusinessUserRepository;
import com.reviewtap.common.ApiException;
import com.reviewtap.user.User;
import com.reviewtap.user.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    private final UserRepository users;
    private final BusinessUserRepository businessUsers;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService attempts;
    /** Hash real de una cadena aleatoria; sólo sirve para igualar tiempos cuando el email no existe. */
    private final String dummyHash;

    public AuthService(UserRepository users, BusinessUserRepository businessUsers, PasswordEncoder passwordEncoder,
            LoginAttemptService attempts) {
        this.users = users;
        this.businessUsers = businessUsers;
        this.passwordEncoder = passwordEncoder;
        this.attempts = attempts;
        this.dummyHash = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public User authenticate(String email, String rawPassword) {
        if (attempts.isBlocked(email)) {
            log.warn("Login bloqueado temporalmente para {}", LoginAttemptService.mask(email));
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_LOCKED",
                    "Demasiados intentos fallidos. Inténtalo de nuevo en unos minutos");
        }
        User user = users.findByEmailIgnoreCase(email).orElse(null);
        // Comparación siempre (aunque no exista el usuario) para no revelar cuentas por tiempo de respuesta.
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        boolean ok = passwordEncoder.matches(rawPassword, hash) && user != null && user.isEnabled();
        if (!ok) {
            attempts.recordFailure(email);
            log.warn("Login fallido para {}", LoginAttemptService.mask(email));
            throw new BadCredentialsException("Credenciales inválidas");
        }
        attempts.reset(email);
        log.info("Login correcto: usuario {}", user.getId());
        return user;
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Usuario"));
        List<BusinessMembership> memberships = businessUsers.findAllByUserId(userId).stream()
                .map(bu -> new BusinessMembership(bu.getBusiness().getId(), bu.getBusiness().getName(),
                        bu.getBusiness().getSlug(), bu.getBusiness().getTimezone(), bu.getBusiness().isActive(),
                        bu.getRole()))
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
        return new MeResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole(), memberships);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Usuario"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("INVALID_CURRENT_PASSWORD", "La contraseña actual no es correcta");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        log.info("Contraseña cambiada por el usuario {}", userId);
    }
}
