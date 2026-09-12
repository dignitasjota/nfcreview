package com.reviewtap.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpsUrlTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://g.page/r/CXyzAbC123/review",
            "https://search.google.com/local/writereview?placeid=ChIJN1t_tDeuEmsRUsoyG83frY4",
            "https://maps.app.goo.gl/AbCdEf",
            "https://www.google.com/maps/place/?q=place_id:ChIJ"})
    void acceptsHttpsUrls(String url) {
        assertThat(HttpsUrl.Validator.isValidHttpsUrl(url)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://g.page/r/X/review",
            "javascript:alert(1)",
            "ftp://example.com",
            "g.page/r/X/review",
            "https://",
            "https://user:pass@evil.example/",
            "https://exa mple.com/",
            "//evil.example",
            "https://evil.example/\nLocation: x"})
    void rejectsEverythingElse(String url) {
        assertThat(HttpsUrl.Validator.isValidHttpsUrl(url)).isFalse();
    }
}
