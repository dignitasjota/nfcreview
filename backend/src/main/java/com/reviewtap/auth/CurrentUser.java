package com.reviewtap.auth;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acceso tipado al usuario autenticado actual. */
public final class CurrentUser {

    private CurrentUser() {}

    public static Optional<AuthPrincipal> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static AuthPrincipal require() {
        return find().orElseThrow(() -> new IllegalStateException("No hay usuario autenticado"));
    }
}
