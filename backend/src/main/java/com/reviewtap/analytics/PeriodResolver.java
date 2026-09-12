package com.reviewtap.analytics;

import com.reviewtap.common.ApiException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Traduce los presets del selector temporal a rangos concretos en la zona horaria del negocio.
 * El periodo de comparación tiene la misma duración e inmediatamente anterior (o el mes natural
 * anterior para los presets mensuales).
 */
@Component
public class PeriodResolver {

    public static final int MAX_CUSTOM_DAYS = 366;

    private final Clock clock;

    public PeriodResolver() {
        this(Clock.systemUTC());
    }

    PeriodResolver(Clock clock) {
        this.clock = clock;
    }

    public Period resolve(String period, LocalDate from, LocalDate to, ZoneId zone) {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        String key = period == null || period.isBlank() ? "30d" : period.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "today" -> new Period(key, today, today, zone);
            case "7d" -> new Period(key, today.minusDays(6), today, zone);
            case "30d" -> new Period(key, today.minusDays(29), today, zone);
            case "this_month" -> new Period(key, today.withDayOfMonth(1), today, zone);
            case "last_month" -> {
                YearMonth prev = YearMonth.from(today).minusMonths(1);
                yield new Period(key, prev.atDay(1), prev.atEndOfMonth(), zone);
            }
            case "custom" -> {
                if (from == null || to == null) {
                    throw ApiException.badRequest("INVALID_PERIOD", "Indica fecha de inicio y fin");
                }
                if (to.isBefore(from)) {
                    throw ApiException.badRequest("INVALID_PERIOD", "La fecha de fin debe ser posterior a la de inicio");
                }
                if (to.toEpochDay() - from.toEpochDay() + 1 > MAX_CUSTOM_DAYS) {
                    throw ApiException.badRequest("INVALID_PERIOD", "El rango máximo es de " + MAX_CUSTOM_DAYS + " días");
                }
                yield new Period(key, from, to, zone);
            }
            default -> throw ApiException.badRequest("INVALID_PERIOD", "Periodo no reconocido");
        };
    }

    public Period previous(Period p) {
        if ("this_month".equals(p.key()) || "last_month".equals(p.key())) {
            YearMonth prev = YearMonth.from(p.fromDate()).minusMonths(1);
            LocalDate end = "this_month".equals(p.key())
                    ? prev.atDay(Math.min(p.toDate().getDayOfMonth(), prev.lengthOfMonth()))
                    : prev.atEndOfMonth();
            return new Period("previous", prev.atDay(1), end, p.zone());
        }
        long days = p.days();
        return new Period("previous", p.fromDate().minusDays(days), p.fromDate().minusDays(1), p.zone());
    }
}
