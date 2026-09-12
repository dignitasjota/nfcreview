package com.reviewtap.analytics;

import com.reviewtap.analytics.AnalyticsDtos.DeviceStatsResponse;
import com.reviewtap.analytics.AnalyticsDtos.RecentRow;
import com.reviewtap.analytics.AnalyticsDtos.SummaryResponse;
import com.reviewtap.analytics.AnalyticsDtos.TimelineResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Métricas de un negocio. Parámetros comunes: {@code period=today|7d|30d|this_month|last_month|custom}
 * y, para custom, {@code from}/{@code to} (fechas ISO en la zona horaria del negocio).
 */
@Tag(name = "Analítica")
@RestController
@RequestMapping("/api/businesses/{businessId}/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    @PreAuthorize("@access.canRead(#businessId)")
    public SummaryResponse summary(@PathVariable UUID businessId,
            @Parameter(description = "today | 7d | 30d | this_month | last_month | custom")
            @RequestParam(defaultValue = "30d") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.summary(businessId, period, from, to);
    }

    @GetMapping("/timeline")
    @PreAuthorize("@access.canRead(#businessId)")
    public TimelineResponse timeline(@PathVariable UUID businessId,
            @RequestParam(defaultValue = "30d") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.timeline(businessId, period, from, to);
    }

    @GetMapping("/devices")
    @PreAuthorize("@access.canRead(#businessId)")
    public DeviceStatsResponse devices(@PathVariable UUID businessId,
            @RequestParam(defaultValue = "30d") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.devices(businessId, period, from, to);
    }

    @GetMapping("/recent")
    @PreAuthorize("@access.canRead(#businessId)")
    public List<RecentRow> recent(@PathVariable UUID businessId, @RequestParam(defaultValue = "20") int limit) {
        return analyticsService.recent(businessId, limit);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    @PreAuthorize("@access.canRead(#businessId)")
    public void export(@PathVariable UUID businessId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID deviceId,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"interacciones-" + from + "-" + to + ".csv\"");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
        analyticsService.exportCsv(businessId, from, to, deviceId, writer);
    }
}
