package com.reviewtap.device;

import java.util.UUID;

/** Proyección mínima para resolver un redirect con una sola consulta. */
public record DeviceRedirectTarget(UUID deviceId, boolean deviceActive, boolean businessActive, String targetUrl) {}
