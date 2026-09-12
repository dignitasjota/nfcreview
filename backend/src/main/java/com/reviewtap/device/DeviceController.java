package com.reviewtap.device;

import com.reviewtap.device.DeviceDtos.DeviceCreateRequest;
import com.reviewtap.device.DeviceDtos.DeviceResponse;
import com.reviewtap.device.DeviceDtos.DeviceStatusRequest;
import com.reviewtap.device.DeviceDtos.DeviceUpdateRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dispositivos")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final QrCodeService qrCodeService;

    @GetMapping("/businesses/{businessId}/devices")
    @PreAuthorize("@access.canRead(#businessId)")
    public List<DeviceResponse> list(@PathVariable UUID businessId) {
        return deviceService.listByBusiness(businessId);
    }

    @PostMapping("/businesses/{businessId}/devices")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceResponse create(@PathVariable UUID businessId, @Valid @RequestBody DeviceCreateRequest request) {
        return deviceService.create(businessId, request);
    }

    @GetMapping("/devices/{id}")
    @PreAuthorize("@access.canReadDevice(#id)")
    public DeviceResponse get(@PathVariable UUID id) {
        return deviceService.get(id);
    }

    @PutMapping("/devices/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DeviceResponse update(@PathVariable UUID id, @Valid @RequestBody DeviceUpdateRequest request) {
        return deviceService.update(id, request);
    }

    @PatchMapping("/devices/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public DeviceResponse setStatus(@PathVariable UUID id, @Valid @RequestBody DeviceStatusRequest request) {
        return deviceService.setActive(id, request.active());
    }

    /** PNG del QR (contenido: la URL {@code ?src=qr}). {@code size} entre 128 y 2048 px. */
    @GetMapping(value = "/devices/{id}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("@access.canReadDevice(#id)")
    public ResponseEntity<byte[]> qrPng(@PathVariable UUID id, @RequestParam(defaultValue = "512") int size,
            @RequestParam(defaultValue = "false") boolean download) {
        Device device = deviceService.find(id);
        int px = Math.max(128, Math.min(2048, size));
        byte[] png = qrCodeService.png(deviceService.qrUrl(device), px);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .headers(h -> {
                    if (download) {
                        h.set(HttpHeaders.CONTENT_DISPOSITION, attachment(device, "png"));
                    }
                })
                .body(png);
    }

    @GetMapping(value = "/devices/{id}/qr.svg", produces = "image/svg+xml")
    @PreAuthorize("@access.canReadDevice(#id)")
    public ResponseEntity<byte[]> qrSvg(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean download) {
        Device device = deviceService.find(id);
        byte[] svg = qrCodeService.svg(deviceService.qrUrl(device)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .headers(h -> {
                    if (download) {
                        h.set(HttpHeaders.CONTENT_DISPOSITION, attachment(device, "svg"));
                    }
                })
                .body(svg);
    }

    private static String attachment(Device device, String ext) {
        String safe = device.getName().replaceAll("[^A-Za-z0-9._-]+", "_");
        return "attachment; filename=\"qr-" + safe + "-" + device.getPublicCode() + "." + ext + "\"";
    }
}
