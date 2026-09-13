package com.reviewtap.device;

import com.reviewtap.analytics.AnalyticsRepository;
import com.reviewtap.business.Business;
import com.reviewtap.business.BusinessRepository;
import com.reviewtap.common.ApiException;
import com.reviewtap.device.DeviceDtos.DeviceCreateRequest;
import com.reviewtap.device.DeviceDtos.DeviceResponse;
import com.reviewtap.device.DeviceDtos.DeviceUpdateRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private static final Duration RECENT_WINDOW = Duration.ofDays(30);

    private final DeviceRepository devices;
    private final BusinessRepository businesses;
    private final AnalyticsRepository analytics;
    private final PublicCodeGenerator codes;
    private final DeviceUrls urls;

    @Transactional(readOnly = true)
    public List<DeviceResponse> listByBusiness(UUID businessId) {
        if (!businesses.existsById(businessId)) {
            throw ApiException.notFound("Negocio");
        }
        return withCounts(devices.findAllByBusinessIdOrderByCreatedAtAsc(businessId));
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> listAll() {
        return withCounts(devices.findAllWithBusiness());
    }

    @Transactional(readOnly = true)
    public DeviceResponse get(UUID id) {
        return withCounts(List.of(find(id))).get(0);
    }

    @Transactional(readOnly = true)
    public Device find(UUID id) {
        return devices.findWithBusinessById(id).orElseThrow(() -> ApiException.notFound("Dispositivo"));
    }

    @Transactional
    public DeviceResponse create(UUID businessId, DeviceCreateRequest req) {
        Business business = businesses.findById(businessId).orElseThrow(() -> ApiException.notFound("Negocio"));
        Device d = new Device();
        d.setBusiness(business);
        d.setPublicCode(uniqueCode());
        d.setName(req.name().trim());
        d.setLocationDescription(blankToNull(req.locationDescription()));
        d.setType(req.type());
        d.setActive(true);
        d = devices.save(d);
        log.info("Dispositivo {} creado para el negocio {}", d.getId(), businessId);
        return toResponse(d, 0);
    }

    @Transactional
    public DeviceResponse update(UUID id, DeviceUpdateRequest req) {
        Device d = find(id);
        d.setName(req.name().trim());
        d.setLocationDescription(blankToNull(req.locationDescription()));
        d.setType(req.type());
        log.info("Dispositivo {} actualizado", id);
        return get(id);
    }

    @Transactional
    public DeviceResponse setActive(UUID id, boolean active) {
        Device d = find(id);
        d.setActive(active);
        log.info("Dispositivo {} {}", id, active ? "activado" : "desactivado");
        return get(id);
    }

    public String nfcUrl(Device d) {
        return urls.nfc(d.getPublicCode());
    }

    public String qrUrl(Device d) {
        return urls.qr(d.getPublicCode());
    }

    private List<DeviceResponse> withCounts(List<Device> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = list.stream().map(Device::getId).toList();
        Map<UUID, Long> counts = analytics.countByDeviceSince(ids, Instant.now().minus(RECENT_WINDOW)).stream()
                .collect(Collectors.toMap(AnalyticsRepository.DeviceCount::getDeviceId,
                        AnalyticsRepository.DeviceCount::getTotal, (a, b) -> a, java.util.HashMap::new));
        return list.stream().map(d -> toResponse(d, counts.getOrDefault(d.getId(), 0L))).toList();
    }

    private String uniqueCode() {
        for (int i = 0; i < 5; i++) {
            String code = codes.generate();
            if (!devices.existsByPublicCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("No se pudo generar un código público único");
    }

    private DeviceResponse toResponse(Device d, long last30) {
        return new DeviceResponse(d.getId(), d.getBusiness().getId(), d.getBusiness().getName(),
                d.getBusiness().getTimezone(), d.getPublicCode(),
                d.getName(), d.getLocationDescription(), d.getType(), d.isActive(), urls.nfc(d.getPublicCode()),
                urls.qr(d.getPublicCode()), last30, d.getCreatedAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

}
