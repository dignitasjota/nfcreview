package com.reviewtap.user;

import com.reviewtap.business.BusinessRole;
import com.reviewtap.common.Passwords;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {}

    public record UserBusinessRef(UUID id, String name, BusinessRole role) {}

    public record UserResponse(UUID id, String email, String firstName, String lastName, UserRole role,
            boolean enabled, Instant createdAt, List<UserBusinessRef> businesses) {}

    public record UserCreateRequest(
            @NotBlank(message = "El email es obligatorio") @Email(message = "Email no válido")
            @Size(max = 255) String email,
            @Pattern(regexp = Passwords.PATTERN, message = Passwords.MESSAGE) String password,
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @NotNull(message = "El rol es obligatorio") UserRole role) {}

    /** Contraseña generada sólo se devuelve una vez, en la creación. */
    public record UserCreatedResponse(UserResponse user, String generatedPassword) {}

    public record UserStatusRequest(@NotNull Boolean enabled) {}

    public record ResetPasswordRequest(
            @NotBlank @Pattern(regexp = Passwords.PATTERN, message = Passwords.MESSAGE) String newPassword) {}
}
