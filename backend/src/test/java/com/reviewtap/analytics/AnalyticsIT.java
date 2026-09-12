package com.reviewtap.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reviewtap.AbstractIntegrationTest;
import com.reviewtap.analytics.AnalyticsDtos.SummaryResponse;
import com.reviewtap.business.Business;
import com.reviewtap.business.BusinessRole;
import com.reviewtap.device.Device;
import com.reviewtap.interaction.InteractionType;
import com.reviewtap.user.User;
import com.reviewtap.user.UserRole;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnalyticsIT extends AbstractIntegrationTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    @Autowired AnalyticsService analyticsService;

    private Business business;
    private Device counter;
    private Device entrance;
    private Cookie session;
    private LocalDate today;

    @BeforeEach
    void setUp() throws Exception {
        User pepe = user("pepe@test.local", UserRole.BUSINESS_USER);
        business = business("Barbería", "https://g.page/r/A/review");
        member(pepe, business, BusinessRole.OWNER);
        counter = device(business, "Mostrador");
        entrance = device(business, "Entrada");
        Device unused = device(business, "Sin uso");
        Business other = business("Otro", "https://g.page/r/B/review");
        Device foreign = device(other, "Ajeno");
        session = login("pepe@test.local");
        today = LocalDate.now(MADRID);

        // Periodo actual (últimos 7 días): 3 NFC + 2 QR en mostrador, 1 QR en entrada = 6
        at(counter, InteractionType.NFC, today, 10);
        at(counter, InteractionType.NFC, today, 11);
        at(counter, InteractionType.NFC, today.minusDays(3), 12);
        at(counter, InteractionType.QR, today.minusDays(6), 9);
        at(counter, InteractionType.QR, today.minusDays(6), 20);
        at(entrance, InteractionType.QR, today.minusDays(1), 18);
        // Periodo anterior (7 días antes): 3 interacciones
        at(counter, InteractionType.NFC, today.minusDays(7), 10);
        at(entrance, InteractionType.QR, today.minusDays(9), 10);
        at(entrance, InteractionType.QR, today.minusDays(13), 10);
        // Fuera de ambos periodos y de otro negocio: no deben contar
        at(counter, InteractionType.NFC, today.minusDays(14), 10);
        at(foreign, InteractionType.NFC, today, 10);
        at(unused, InteractionType.NFC, today.minusDays(40), 10);
    }

    private void at(Device d, InteractionType type, LocalDate day, int hour) {
        Instant instant = day.atTime(LocalTime.of(hour, 30)).atZone(MADRID).toInstant();
        interaction(d, type, instant);
    }

    @Test
    void summaryAggregatesCurrentAndPreviousPeriod() {
        SummaryResponse s = analyticsService.summary(business.getId(), "7d", null, null);
        assertThat(s.current().total()).isEqualTo(6);
        assertThat(s.current().nfc()).isEqualTo(3);
        assertThat(s.current().qr()).isEqualTo(3);
        assertThat(s.previous().total()).isEqualTo(3);
        assertThat(s.changePercent()).isEqualTo(100.0);
        assertThat(s.topDevice().name()).isEqualTo("Mostrador");
        assertThat(s.topDevice().total()).isEqualTo(5);
        assertThat(s.period().from()).isEqualTo(today.minusDays(6));
        assertThat(s.previousPeriod().to()).isEqualTo(today.minusDays(7));
    }

    @Test
    void changePercentIsNullWithoutBaseline() {
        SummaryResponse s = analyticsService.summary(business.getId(), "today", null, null);
        assertThat(s.current().total()).isEqualTo(2);
        // Ayer sólo hay 1 interacción (entrada) → +100 %; anteayer... comprobamos el caso sin base con "custom".
        SummaryResponse empty = analyticsService.summary(business.getId(), "custom", today.minusDays(50),
                today.minusDays(45));
        assertThat(empty.current().total()).isZero();
        assertThat(empty.changePercent()).isNull();
        assertThat(empty.topDevice()).isNull();
    }

    @Test
    void timelineFillsEveryDayOfThePeriod() throws Exception {
        mvc.perform(get("/api/businesses/{id}/analytics/timeline", business.getId()).cookie(session).param("period", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(7))
                .andExpect(jsonPath("$.points[0].date").value(today.minusDays(6).toString()))
                .andExpect(jsonPath("$.points[0].total").value(2))
                .andExpect(jsonPath("$.points[0].qr").value(2))
                .andExpect(jsonPath("$.points[6].date").value(today.toString()))
                .andExpect(jsonPath("$.points[6].nfc").value(2))
                .andExpect(jsonPath("$.points[1].total").value(0));
    }

    @Test
    void deviceStatsIncludeUnusedDevicesAndSortByTotal() throws Exception {
        mvc.perform(get("/api/businesses/{id}/analytics/devices", business.getId()).cookie(session).param("period", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devices.length()").value(3))
                .andExpect(jsonPath("$.devices[0].name").value("Mostrador"))
                .andExpect(jsonPath("$.devices[0].nfc").value(3))
                .andExpect(jsonPath("$.devices[0].qr").value(2))
                .andExpect(jsonPath("$.devices[0].total").value(5))
                .andExpect(jsonPath("$.devices[1].name").value("Entrada"))
                .andExpect(jsonPath("$.devices[2].name").value("Sin uso"))
                .andExpect(jsonPath("$.devices[2].total").value(0));
    }

    @Test
    void recentInteractionsAreScopedToBusinessAndAnonymous() throws Exception {
        mvc.perform(get("/api/businesses/{id}/analytics/recent", business.getId()).cookie(session).param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].deviceName").value("Mostrador"))
                .andExpect(jsonPath("$[0].type").value("NFC"))
                .andExpect(jsonPath("$[0].ip").doesNotExist());
    }

    @Test
    void csvExportRespectsRangeAndDeviceFilter() throws Exception {
        String from = today.minusDays(6).toString();
        String to = today.toString();
        String csv = mvc.perform(get("/api/businesses/{id}/analytics/export.csv", business.getId()).cookie(session)
                        .param("from", from).param("to", to))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(csv).startsWith("﻿fecha_hora;dispositivo;ubicacion;tipo");
        assertThat(csv.strip().split("\r\n")).hasSize(7); // cabecera + 6

        String onlyEntrance = mvc.perform(get("/api/businesses/{id}/analytics/export.csv", business.getId())
                        .cookie(session).param("from", from).param("to", to)
                        .param("deviceId", entrance.getId().toString()))
                .andReturn().getResponse().getContentAsString();
        assertThat(onlyEntrance.strip().split("\r\n")).hasSize(2);
        assertThat(onlyEntrance).contains(";Entrada;");
    }

    @Test
    void invalidCustomRangeIsRejected() throws Exception {
        mvc.perform(get("/api/businesses/{id}/analytics/summary", business.getId()).cookie(session)
                        .param("period", "custom").param("from", "2026-02-01").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD"));
        mvc.perform(get("/api/businesses/{id}/analytics/summary", business.getId()).cookie(session)
                        .param("period", "siempre"))
                .andExpect(status().isBadRequest());
    }
}
