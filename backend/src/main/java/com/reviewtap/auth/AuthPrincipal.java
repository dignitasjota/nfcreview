package com.reviewtap.auth;

import com.reviewtap.user.UserRole;
import java.util.UUID;

/** Identidad autenticada que viaja en el SecurityContext. */
public record AuthPrincipal(UUID userId, String email, UserRole role, int tokenVersion) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
