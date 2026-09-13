package com.reviewtap.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class DeviceDtos {

    private DeviceDtos() {}

    public record DeviceResponse(UUID id, UUID businessId, String businessName, String businessTimezone,
            String publicCode, String name,
            String locationDescription, DeviceType type, boolean active, String nfcUrl, String qrUrl,
            long interactionsLast30Days, Instant createdAt) {}

    public record DeviceCreateRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String name,
            @Size(max = 200) String locationDescription,
            @NotNull(message = "El tipo es obligatorio") DeviceType type) {}

    public record DeviceUpdateRequest(
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String name,
            @Size(max = 200) String locationDescription,
            @NotNull(message = "El tipo es obligatorio") DeviceType type) {}

    public record DeviceStatusRequest(@NotNull Boolean active) {}
}
