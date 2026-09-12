package com.reviewtap.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reviewtap.common.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PeriodResolverTest {

    // 2026-03-15 23:30 UTC = 2026-03-16 00:30 en Madrid: el "hoy" depende de la zona del negocio.
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-15T23:30:00Z"), ZoneOffset.UTC);
    private final PeriodResolver resolver = new PeriodResolver(clock);
    private final ZoneId madrid = ZoneId.of("Europe/Madrid");

    @Test
    void todayUsesBusinessTimezone() {
        Period p = resolver.resolve("today", null, null, madrid);
        assertThat(p.fromDate()).isEqualTo(LocalDate.of(2026, 3, 16));
        assertThat(p.from()).isEqualTo(Instant.parse("2026-03-15T23:00:00Z"));
        assertThat(p.to()).isEqualTo(Instant.parse("2026-03-16T23:00:00Z"));
        assertThat(resolver.resolve("today", null, null, ZoneOffset.UTC).fromDate()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void sevenAndThirtyDaysIncludeToday() {
        Period p7 = resolver.resolve("7d", null, null, madrid);
        assertThat(p7.days()).isEqualTo(7);
        assertThat(p7.toDate()).isEqualTo(LocalDate.of(2026, 3, 16));
        Period p30 = resolver.resolve("30d", null, null, madrid);
        assertThat(p30.days()).isEqualTo(30);
        assertThat(p30.fromDate()).isEqualTo(LocalDate.of(2026, 2, 15));
    }

    @Test
    void previousPeriodHasSameLengthAndEndsRightBefore() {
        Period p = resolver.resolve("7d", null, null, madrid);
        Period prev = resolver.previous(p);
        assertThat(prev.days()).isEqualTo(7);
        assertThat(prev.toDate()).isEqualTo(p.fromDate().minusDays(1));
        assertThat(prev.to()).isEqualTo(p.from());
    }

    @Test
    void monthPresetsCompareWithPreviousCalendarMonth() {
        Period thisMonth = resolver.resolve("this_month", null, null, madrid);
        assertThat(thisMonth.fromDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(thisMonth.toDate()).isEqualTo(LocalDate.of(2026, 3, 16));
        Period prev = resolver.previous(thisMonth);
        assertThat(prev.fromDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(prev.toDate()).isEqualTo(LocalDate.of(2026, 2, 16));

        Period lastMonth = resolver.resolve("last_month", null, null, madrid);
        assertThat(lastMonth.fromDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(lastMonth.toDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(resolver.previous(lastMonth).fromDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(resolver.previous(lastMonth).toDate()).isEqualTo(LocalDate.of(2026, 1, 31));
    }

    @Test
    void customRangeIsValidated() {
        assertThatThrownBy(() -> resolver.resolve("custom", null, null, madrid)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> resolver.resolve("custom", LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 1), madrid))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> resolver.resolve("custom", LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1), madrid))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> resolver.resolve("all", null, null, madrid)).isInstanceOf(ApiException.class);
        Period ok = resolver.resolve("custom", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), madrid);
        assertThat(ok.days()).isEqualTo(31);
    }
}
