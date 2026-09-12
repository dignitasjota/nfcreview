package com.reviewtap.business;

import com.reviewtap.common.HttpsUrl;
import com.reviewtap.common.Passwords;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class BusinessDtos {

    private BusinessDtos() {}

    public record BusinessResponse(UUID id, String name, String slug, String googleReviewUrl, String logoUrl,
            String address, String phone, String timezone, boolean active, Instant createdAt, long deviceCount) {}

    /** Alta de negocio con propietario opcional en un solo paso (flujo "dar de alta una venta"). */
    public record BusinessCreateRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String name,
            @HttpsUrl @Size(max = 2048) String googleReviewUrl,
            @Size(max = 64) String timezone,
            @Size(max = 255) String address,
            @Size(max = 40) String phone,
            @Email(message = "Email no válido") @Size(max = 255) String ownerEmail,
            @Size(max = 100) String ownerFirstName,
            @Size(max = 100) String ownerLastName,
            @Pattern(regexp = Passwords.PATTERN, message = Passwords.MESSAGE) String ownerPassword) {}

    public record OwnerResult(UUID userId, String email, boolean created, String generatedPassword) {}

    public record BusinessCreatedResponse(BusinessResponse business, OwnerResult owner) {}

    /** Edición completa (ADMIN). */
    public record BusinessUpdateRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String name,
            @HttpsUrl @Size(max = 2048) String googleReviewUrl,
            @HttpsUrl @Size(max = 2048) String logoUrl,
            @Size(max = 255) String address,
            @Size(max = 40) String phone,
            @NotBlank(message = "La zona horaria es obligatoria") @Size(max = 64) String timezone) {}

    /** Edición por el propietario: sin URL de Google ni estado. */
    public record BusinessProfileUpdateRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 150) String name,
            @HttpsUrl @Size(max = 2048) String logoUrl,
            @Size(max = 255) String address,
            @Size(max = 40) String phone,
            @NotBlank(message = "La zona horaria es obligatoria") @Size(max = 64) String timezone) {}

    public record StatusRequest(@NotNull Boolean active) {}

    public record MemberResponse(UUID userId, String email, String fullName, boolean enabled, BusinessRole role,
            Instant since) {}

    public record MemberAddRequest(
            @NotBlank(message = "El email es obligatorio") @Email(message = "Email no válido") String email,
            @NotNull(message = "El rol es obligatorio") BusinessRole role,
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Pattern(regexp = Passwords.PATTERN, message = Passwords.MESSAGE) String password) {}

    public record MemberAddedResponse(MemberResponse member, boolean userCreated, String generatedPassword) {}
}
