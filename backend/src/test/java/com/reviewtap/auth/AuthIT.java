package com.reviewtap.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reviewtap.AbstractIntegrationTest;
import com.reviewtap.business.Business;
import com.reviewtap.business.BusinessRole;
import com.reviewtap.user.User;
import com.reviewtap.user.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthIT extends AbstractIntegrationTest {

    @Test
    void loginSetsHttpOnlyCookieAndReturnsProfileWithBusinesses() throws Exception {
        User pepe = user("pepe@test.local", UserRole.BUSINESS_USER);
        Business b = business("Barbería Pepe", "https://g.page/r/X/review");
        member(pepe, b, BusinessRole.OWNER);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"PEPE@test.local\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("rt_session"))
                .andExpect(cookie().httpOnly("rt_session", true))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=Strict")))
                .andExpect(jsonPath("$.email").value("pepe@test.local"))
                .andExpect(jsonPath("$.role").value("BUSINESS_USER"))
                .andExpect(jsonPath("$.businesses[0].name").value("Barbería Pepe"))
                .andExpect(jsonPath("$.businesses[0].role").value("OWNER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void wrongPasswordReturns401WithoutCookie() throws Exception {
        user("pepe@test.local", UserRole.BUSINESS_USER);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pepe@test.local\",\"password\":\"nope12345\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("rt_session"))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void unknownEmailReturnsSameErrorAsWrongPassword() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@test.local\",\"password\":\"whatever1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void invalidPayloadReturnsValidationError() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void meRequiresSessionAndLogoutClearsIt() throws Exception {
        user("pepe@test.local", UserRole.BUSINESS_USER);
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        Cookie session = login("pepe@test.local");
        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pepe@test.local"));

        mvc.perform(post("/api/auth/logout").cookie(session))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("rt_session", 0));
    }

    @Test
    void disabledUserCannotUseExistingSession() throws Exception {
        User pepe = user("pepe@test.local", UserRole.BUSINESS_USER);
        Cookie session = login("pepe@test.local");
        pepe.setEnabled(false);
        users.save(pepe);

        mvc.perform(get("/api/auth/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedCookieIsRejected() throws Exception {
        user("pepe@test.local", UserRole.BUSINESS_USER);
        Cookie session = login("pepe@test.local");
        Cookie tampered = new Cookie("rt_session", session.getValue().substring(0, session.getValue().length() - 4) + "abcd");
        mvc.perform(get("/api/auth/me").cookie(tampered)).andExpect(status().isUnauthorized());
    }

    @Test
    void accountIsLockedAfterRepeatedFailures() throws Exception {
        user("lock@test.local", UserRole.BUSINESS_USER);
        String bad = "{\"email\":\"lock@test.local\",\"password\":\"wrong1234\"}";
        for (int i = 0; i < 3; i++) { // login-max-attempts = 3 en test
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(bad))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lock@test.local\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_LOCKED"));
    }

    @Test
    void changePasswordRequiresCurrentPassword() throws Exception {
        user("pepe@test.local", UserRole.BUSINESS_USER);
        Cookie session = login("pepe@test.local");

        mvc.perform(post("/api/auth/change-password").cookie(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"mal\",\"newPassword\":\"Nueva1234\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/change-password").cookie(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"corta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.newPassword").exists());
        mvc.perform(post("/api/auth/change-password").cookie(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"Nueva1234\"}"))
                .andExpect(status().isNoContent());

        assertThat(passwordEncoder.matches("Nueva1234",
                users.findByEmailIgnoreCase("pepe@test.local").orElseThrow().getPasswordHash())).isTrue();
    }
}
