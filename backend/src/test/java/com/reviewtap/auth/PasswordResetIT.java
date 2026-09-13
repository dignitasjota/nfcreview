package com.reviewtap.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reviewtap.AbstractIntegrationTest;
import com.reviewtap.user.User;
import com.reviewtap.user.UserRole;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class PasswordResetIT extends AbstractIntegrationTest {

    @Autowired PasswordResetService service;
    @Autowired PasswordResetTokenRepository tokens;

    @Test
    void forgotPasswordAlwaysAnswers204AndStoresOnlyTheHash() throws Exception {
        user("pepe@test.local", UserRole.BUSINESS_USER);
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nadie@test.local\"}")).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"PEPE@test.local\"}")).andExpect(status().isNoContent());
        assertThat(tokens.count()).isEqualTo(1);
        assertThat(tokens.findAll().get(0).getTokenHash()).hasSize(64);
    }

    @Test
    void fullFlowResetsPasswordRevokesSessionsAndBurnsToken() throws Exception {
        User pepe = user("pepe@test.local", UserRole.BUSINESS_USER);
        Cookie oldSession = login("pepe@test.local");
        String token = service.request("pepe@test.local", "203.0.113.1").orElseThrow();

        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"Nueva1234\"}"))
                .andExpect(status().isNoContent());

        assertThat(passwordEncoder.matches("Nueva1234", users.findById(pepe.getId()).orElseThrow().getPasswordHash())).isTrue();
        mvc.perform(get("/api/auth/me").cookie(oldSession)).andExpect(status().isUnauthorized());
        // Segundo uso del mismo token: rechazado.
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"Otra12345\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        // Login con la nueva contraseña.
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pepe@test.local\",\"password\":\"Nueva1234\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void expiredOrUnknownTokensAreRejectedAndWeakPasswordsToo() throws Exception {
        user("pepe@test.local", UserRole.BUSINESS_USER);
        String token = service.request("pepe@test.local", "203.0.113.1").orElseThrow();
        PasswordResetToken stored = tokens.findAll().get(0);
        stored.setExpiresAt(Instant.now().minusSeconds(1));
        tokens.save(stored);

        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"Nueva1234\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"deadbeef\",\"newPassword\":\"Nueva1234\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"corta\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.newPassword").exists());
    }

    @Test
    void requestsAreRateLimitedPerEmail() throws Exception {
        user("limit@test.local", UserRole.BUSINESS_USER);
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"limit@test.local\"}")).andExpect(status().isNoContent());
        }
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"limit@test.local\"}")).andExpect(status().isTooManyRequests());
    }
}
