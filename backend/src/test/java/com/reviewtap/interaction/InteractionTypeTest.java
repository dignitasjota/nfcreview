package com.reviewtap.interaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InteractionTypeTest {

    @Test
    void onlyKnownSourcesAreAccepted() {
        assertThat(InteractionType.fromSource("nfc")).isEqualTo(InteractionType.NFC);
        assertThat(InteractionType.fromSource("QR")).isEqualTo(InteractionType.QR);
        assertThat(InteractionType.fromSource(" qr ")).isEqualTo(InteractionType.QR);
        assertThat(InteractionType.fromSource(null)).isEqualTo(InteractionType.UNKNOWN);
        assertThat(InteractionType.fromSource("")).isEqualTo(InteractionType.UNKNOWN);
        assertThat(InteractionType.fromSource("nfc'; drop table")).isEqualTo(InteractionType.UNKNOWN);
        assertThat(InteractionType.fromSource("sms")).isEqualTo(InteractionType.UNKNOWN);
    }

    @Test
    void userAgentsAreClassifiedCoarsely() {
        assertThat(UserAgentCategory.classify("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0) Mobile Safari"))
                .isEqualTo(UserAgentCategory.MOBILE);
        assertThat(UserAgentCategory.classify("Mozilla/5.0 (Linux; Android 14; Pixel 8) Chrome Mobile"))
                .isEqualTo(UserAgentCategory.MOBILE);
        assertThat(UserAgentCategory.classify("Mozilla/5.0 (iPad; CPU OS 17_0) Safari"))
                .isEqualTo(UserAgentCategory.TABLET);
        assertThat(UserAgentCategory.classify("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome"))
                .isEqualTo(UserAgentCategory.DESKTOP);
        assertThat(UserAgentCategory.classify("Googlebot/2.1 (+http://www.google.com/bot.html)"))
                .isEqualTo(UserAgentCategory.BOT);
        assertThat(UserAgentCategory.classify(null)).isEqualTo(UserAgentCategory.UNKNOWN);
    }

    @Test
    void refererKeepsOnlyOrigin() {
        assertThat(InteractionRecorder.refererOrigin("https://example.com/path?q=secret#x")).isEqualTo("https://example.com");
        assertThat(InteractionRecorder.refererOrigin("not a url")).isNull();
        assertThat(InteractionRecorder.refererOrigin(null)).isNull();
    }
}
