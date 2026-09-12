package com.reviewtap.analytics;

import com.reviewtap.interaction.InteractionType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AnalyticsDtos {

    private AnalyticsDtos() {}

    public record PeriodInfo(String key, LocalDate from, LocalDate to, String timezone) {}

    public record Counts(long total, long nfc, long qr, long unknown) {}

    public record TopDevice(UUID id, String name, long total) {}

    /** changePercent es null cuando el periodo anterior no tiene datos (no hay base de comparación). */
    public record SummaryResponse(PeriodInfo period, PeriodInfo previousPeriod, Counts current, Counts previous,
            Double changePercent, TopDevice topDevice) {}

    public record TimelinePoint(LocalDate date, long total, long nfc, long qr) {}

    public record TimelineResponse(PeriodInfo period, List<TimelinePoint> points) {}

    public record DeviceStatsRow(UUID deviceId, String name, String location, boolean active, long nfc, long qr,
            long unknown, long total) {}

    public record DeviceStatsResponse(PeriodInfo period, List<DeviceStatsRow> devices) {}

    public record RecentRow(Instant createdAt, UUID deviceId, String deviceName, String location,
            InteractionType type) {}

    public record TopBusiness(UUID id, String name, long total) {}

    public record AdminSummaryResponse(PeriodInfo period, PeriodInfo previousPeriod, Counts current, Counts previous,
            Double changePercent, long businesses, long activeBusinesses, long devices, long activeDevices,
            List<TopBusiness> topBusinesses, List<TimelinePoint> timeline) {}
}
