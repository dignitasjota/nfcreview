package com.reviewtap.analytics;

import com.reviewtap.analytics.AnalyticsDtos.AdminSummaryResponse;
import com.reviewtap.analytics.AnalyticsDtos.Counts;
import com.reviewtap.analytics.AnalyticsDtos.DeviceStatsResponse;
import com.reviewtap.analytics.AnalyticsDtos.DeviceStatsRow;
import com.reviewtap.analytics.AnalyticsDtos.PeriodInfo;
import com.reviewtap.analytics.AnalyticsDtos.RecentRow;
import com.reviewtap.analytics.AnalyticsDtos.SummaryResponse;
import com.reviewtap.analytics.AnalyticsDtos.TimelinePoint;
import com.reviewtap.analytics.AnalyticsDtos.TimelineResponse;
import com.reviewtap.analytics.AnalyticsDtos.TopBusiness;
import com.reviewtap.analytics.AnalyticsDtos.TopDevice;
import com.reviewtap.analytics.AnalyticsRepository.DayCount;
import com.reviewtap.analytics.AnalyticsRepository.DeviceStats;
import com.reviewtap.analytics.AnalyticsRepository.Totals;
import com.reviewtap.business.Business;
import com.reviewtap.business.BusinessRepository;
import com.reviewtap.common.ApiException;
import com.reviewtap.device.DeviceRepository;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AnalyticsRepository analytics;
    private final BusinessRepository businesses;
    private final DeviceRepository devices;
    private final PeriodResolver periods;

    @Transactional(readOnly = true)
    public SummaryResponse summary(UUID businessId, String periodKey, LocalDate from, LocalDate to) {
        Business business = business(businessId);
        Period period = periods.resolve(periodKey, from, to, business.zoneId());
        Period previous = periods.previous(period);

        Counts current = counts(analytics.totals(businessId, period.from(), period.to()));
        Counts prev = counts(analytics.totals(businessId, previous.from(), previous.to()));

        TopDevice top = analytics.deviceStats(businessId, period.from(), period.to()).stream()
                .filter(d -> d.getTotal() > 0)
                .findFirst()
                .map(d -> new TopDevice(d.getDeviceId(), d.getName(), d.getTotal()))
                .orElse(null);

        return new SummaryResponse(info(period), info(previous), current, prev,
                changePercent(current.total(), prev.total()), top);
    }

    @Transactional(readOnly = true)
    public TimelineResponse timeline(UUID businessId, String periodKey, LocalDate from, LocalDate to) {
        Business business = business(businessId);
        Period period = periods.resolve(periodKey, from, to, business.zoneId());
        List<DayCount> rows = analytics.timeline(businessId, period.from(), period.to(), business.getTimezone());
        return new TimelineResponse(info(period), fillDays(period, rows));
    }

    @Transactional(readOnly = true)
    public DeviceStatsResponse devices(UUID businessId, String periodKey, LocalDate from, LocalDate to) {
        Business business = business(businessId);
        Period period = periods.resolve(periodKey, from, to, business.zoneId());
        List<DeviceStatsRow> rows = analytics.deviceStats(businessId, period.from(), period.to()).stream()
                .map(AnalyticsService::row)
                .toList();
        return new DeviceStatsResponse(info(period), rows);
    }

    @Transactional(readOnly = true)
    public List<RecentRow> recent(UUID businessId, int limit) {
        business(businessId);
        int size = Math.max(1, Math.min(100, limit));
        return analytics.recent(businessId, PageRequest.of(0, size)).stream()
                .map(r -> new RecentRow(r.createdAt(), r.deviceId(), r.deviceName(), r.location(), r.type()))
                .toList();
    }

    /**
     * Exporta CSV (UTF-8 con BOM para Excel, separador ';'). Las fechas se formatean en la zona
     * horaria del negocio. Se recorre el resultado en streaming para no cargarlo en memoria.
     */
    @Transactional(readOnly = true)
    public void exportCsv(UUID businessId, LocalDate from, LocalDate to, UUID deviceId, Writer out)
            throws IOException {
        Business business = business(businessId);
        Period period = periods.resolve("custom", from, to, business.zoneId());
        if (deviceId != null) {
            boolean belongs = devices.findById(deviceId)
                    .map(d -> d.getBusiness().getId().equals(businessId))
                    .orElse(false);
            if (!belongs) {
                throw ApiException.notFound("Dispositivo");
            }
        }
        ZoneId zone = business.zoneId();
        out.write('\uFEFF');
        out.write("fecha_hora;dispositivo;ubicacion;tipo\r\n");
        try (Stream<ExportRow> rows = deviceId == null
                ? analytics.streamForExport(businessId, period.from(), period.to())
                : analytics.streamForExportByDevice(businessId, deviceId, period.from(), period.to())) {
            var it = rows.iterator();
            while (it.hasNext()) {
                ExportRow r = it.next();
                out.write(csv(CSV_TIME.format(r.createdAt().atZone(zone))));
                out.write(';');
                out.write(csv(r.deviceName()));
                out.write(';');
                out.write(csv(r.location()));
                out.write(';');
                out.write(csv(r.type().name()));
                out.write("\r\n");
            }
        }
        out.flush();
    }

    @Transactional(readOnly = true)
    public AdminSummaryResponse adminSummary(String periodKey, LocalDate from, LocalDate to) {
        ZoneId zone = ZoneId.of(Business.DEFAULT_TIMEZONE);
        Period period = periods.resolve(periodKey, from, to, zone);
        Period previous = periods.previous(period);
        Counts current = counts(analytics.globalTotals(period.from(), period.to()));
        Counts prev = counts(analytics.globalTotals(previous.from(), previous.to()));
        List<TopBusiness> top = analytics.topBusinesses(period.from(), period.to(), 5).stream()
                .map(b -> new TopBusiness(b.getBusinessId(), b.getName(), b.getTotal()))
                .toList();
        List<TimelinePoint> timeline = fillDays(period,
                analytics.globalTimeline(period.from(), period.to(), zone.getId()));
        return new AdminSummaryResponse(info(period), info(previous), current, prev,
                changePercent(current.total(), prev.total()), businesses.count(), businesses.countByActiveTrue(),
                devices.count(), devices.countByActiveTrue(), top, timeline);
    }

    // ---- helpers ----

    private Business business(UUID id) {
        return businesses.findById(id).orElseThrow(() -> ApiException.notFound("Negocio"));
    }

    static List<TimelinePoint> fillDays(Period period, List<DayCount> rows) {
        Map<LocalDate, DayCount> byDay = rows.stream()
                .collect(Collectors.toMap(r -> LocalDate.parse(r.getDay()), Function.identity(), (a, b) -> a));
        List<TimelinePoint> points = new ArrayList<>((int) period.days());
        for (LocalDate d = period.fromDate(); !d.isAfter(period.toDate()); d = d.plusDays(1)) {
            DayCount c = byDay.get(d);
            points.add(c == null ? new TimelinePoint(d, 0, 0, 0)
                    : new TimelinePoint(d, c.getTotal(), c.getNfc(), c.getQr()));
        }
        return points;
    }

    static Double changePercent(long current, long previous) {
        if (previous == 0) {
            return null;
        }
        return Math.round(((double) (current - previous) / previous) * 1000.0) / 10.0;
    }

    private static Counts counts(Totals t) {
        return t == null ? new Counts(0, 0, 0, 0) : new Counts(t.getTotal(), t.getNfc(), t.getQr(), t.getUnknown());
    }

    private static DeviceStatsRow row(DeviceStats d) {
        return new DeviceStatsRow(d.getDeviceId(), d.getName(), d.getLocation(), Boolean.TRUE.equals(d.getActive()),
                d.getNfc(), d.getQr(), d.getUnknown(), d.getTotal());
    }

    private static PeriodInfo info(Period p) {
        return new PeriodInfo(p.key(), p.fromDate(), p.toDate(), p.zone().getId());
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        boolean quote = value.contains(";") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String v = value.replace("\"", "\"\"");
        return quote ? "\"" + v + "\"" : v;
    }
}
