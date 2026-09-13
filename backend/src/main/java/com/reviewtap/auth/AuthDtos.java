package com.reviewtap.auth;

import com.reviewtap.business.BusinessRole;
import com.reviewtap.common.Passwords;
import com.reviewtap.user.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "El email es obligatorio") @Email(message = "Email no válido") String email,
            @NotBlank(message = "La contraseña es obligatoria") String password) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "La contraseña actual es obligatoria") String currentPassword,
            @NotBlank(message = "La nueva contraseña es obligatoria")
            @Pattern(regexp = Passwords.PATTERN, message = Passwords.MESSAGE) String newPassword) {}

    public record ForgotPasswordRequest(
            @NotBlank(message = "El email es obligatorio") @Email(message = "Email no válido") String email) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Falta el token") String token,
            @NotBlank(message = "La nueva contraseña es obligatoria")
            @Pattern(regexp = Passwords.PATTERN, message = Passwords.MESSAGE) String newPassword) {}

    public record BusinessMembership(UUID id, String name, String slug, String timezone, boolean active,
            BusinessRole role) {}

    public record MeResponse(UUID id, String email, String firstName, String lastName, UserRole role,
            List<BusinessMembership> businesses) {}
}
