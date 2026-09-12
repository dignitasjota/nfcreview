package com.reviewtap.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reviewtap.AbstractIntegrationTest;
import com.reviewtap.business.Business;
import com.reviewtap.device.Device;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class RedirectControllerIT extends AbstractIntegrationTest {

    private static final String TARGET = "https://g.page/r/TEST/review";
    private static final String UA_IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Mobile Safari";
    private static final String UA_ANDROID = "Mozilla/5.0 (Linux; Android 14) Chrome Mobile";

    private Business business;
    private Device device;

    @BeforeEach
    void setUp() {
        business = business("Barbería Test", TARGET);
        device = device(business, "Mostrador");
    }

    @Test
    void redirectsToGoogleAndRecordsNfcInteraction() throws Exception {
        mvc.perform(get("/d/{code}", device.getPublicCode()).param("src", "nfc").header("User-Agent", UA_IPHONE))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", TARGET))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(""));

        List<Interaction> all = interactions.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getDeviceId()).isEqualTo(device.getId());
        assertThat(all.get(0).getInteractionType()).isEqualTo(InteractionType.NFC);
        assertThat(all.get(0).getUserAgentCategory()).isEqualTo(UserAgentCategory.MOBILE);
    }

    @Test
    void distinguishesQrAndUnknownSources() throws Exception {
        mvc.perform(get("/d/{code}", device.getPublicCode()).param("src", "qr").header("User-Agent", UA_IPHONE))
                .andExpect(status().isFound());
        mvc.perform(get("/d/{code}", device.getPublicCode()).param("src", "javascript:x")
                        .header("User-Agent", UA_ANDROID))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", TARGET));

        assertThat(interactions.findAll()).extracting(Interaction::getInteractionType)
                .containsExactlyInAnyOrder(InteractionType.QR, InteractionType.UNKNOWN);
    }

    @Test
    void unknownCodeReturns404HtmlWithoutRedirect() throws Exception {
        mvc.perform(get("/d/{code}", "NoExiste99"))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
        assertThat(interactions.count()).isZero();
    }

    @Test
    void malformedCodeIsRejectedWithoutTouchingDatabase() throws Exception {
        mvc.perform(get("/d/{code}", "ab")).andExpect(status().isNotFound());
        mvc.perform(get("/d/{code}", "código-con-ñ")).andExpect(status().isNotFound());
    }

    @Test
    void inactiveDeviceDoesNotRedirect() throws Exception {
        device.setActive(false);
        devices.save(device);

        mvc.perform(get("/d/{code}", device.getPublicCode()).param("src", "nfc"))
                .andExpect(status().isGone())
                .andExpect(header().doesNotExist("Location"));
        assertThat(interactions.count()).isZero();
    }

    @Test
    void inactiveBusinessDoesNotRedirect() throws Exception {
        business.setActive(false);
        businesses.save(business);

        mvc.perform(get("/d/{code}", device.getPublicCode()).param("src", "qr"))
                .andExpect(status().isGone())
                .andExpect(header().doesNotExist("Location"));
        assertThat(interactions.count()).isZero();
    }

    @Test
    void missingTargetUrlShowsErrorPage() throws Exception {
        business.setGoogleReviewUrl(null);
        businesses.save(business);

        mvc.perform(get("/d/{code}", device.getPublicCode()))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void repeatedTapsFromSameClientCountOnce() throws Exception {
        for (int i = 0; i < 4; i++) {
            mvc.perform(get("/d/{code}", device.getPublicCode()).param("src", "nfc").header("User-Agent", UA_IPHONE))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", TARGET));
        }
        // Otro cliente (otro UA) sí cuenta.
        mvc.perform(get("/d/{code}", device.getPublicCode()).param("src", "nfc").header("User-Agent", UA_ANDROID))
                .andExpect(status().isFound());

        assertThat(interactions.count()).isEqualTo(2);
    }

    @Test
    void sameClientOnDifferentDevicesCountsPerDevice() throws Exception {
        Device second = device(business, "Entrada");
        mvc.perform(get("/d/{code}", device.getPublicCode()).header("User-Agent", UA_IPHONE)).andExpect(status().isFound());
        mvc.perform(get("/d/{code}", second.getPublicCode()).header("User-Agent", UA_IPHONE)).andExpect(status().isFound());
        assertThat(interactions.count()).isEqualTo(2);
    }

    @Test
    void perClientCapLimitsRecordingAcrossDevices() throws Exception {
        // app.interactions.client-rate-limit = 5 en el perfil test.
        for (int i = 0; i < 8; i++) {
            Device d = device(business, "D" + i);
            mvc.perform(get("/d/{code}", d.getPublicCode()).header("User-Agent", "cap-client-ua"))
                    .andExpect(status().isFound());
        }
        assertThat(interactions.count()).isEqualTo(5);
    }
}
