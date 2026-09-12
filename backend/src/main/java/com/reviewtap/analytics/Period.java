package com.reviewtap.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Rango [from, to) en UTC más las fechas locales (inclusive) que lo originaron. */
public record Period(String key, LocalDate fromDate, LocalDate toDate, ZoneId zone) {

    public Instant from() {
        return fromDate.atStartOfDay(zone).toInstant();
    }

    /** Exclusivo: inicio del día siguiente a {@code toDate}. */
    public Instant to() {
        return toDate.plusDays(1).atStartOfDay(zone).toInstant();
    }

    public long days() {
        return toDate.toEpochDay() - fromDate.toEpochDay() + 1;
    }
}
