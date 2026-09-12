package com.reviewtap.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.reviewtap.AbstractIntegrationTest;
import com.reviewtap.business.Business;
import com.reviewtap.business.BusinessRole;
import com.reviewtap.device.Device;
import com.reviewtap.user.User;
import com.reviewtap.user.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Un negocio jamás puede consultar datos de otro; las operaciones de alta son sólo de ADMIN. */
class TenantAuthorizationIT extends AbstractIntegrationTest {

    private Business mine;
    private Business other;
    private Device myDevice;
    private Device otherDevice;
    private Cookie pepe;
    private Cookie manager;
    private Cookie admin;

    @BeforeEach
    void setUp() throws Exception {
        User owner = user("pepe@test.local", UserRole.BUSINESS_USER);
        User mgr = user("manager@test.local", UserRole.BUSINESS_USER);
        user("admin@test.local", UserRole.ADMIN);
        mine = business("Barbería Pepe", "https://g.page/r/A/review");
        other = business("Cafetería Ajena", "https://g.page/r/B/review");
        member(owner, mine, BusinessRole.OWNER);
        member(mgr, mine, BusinessRole.MANAGER);
        myDevice = device(mine, "Mostrador");
        otherDevice = device(other, "Barra");
        pepe = login("pepe@test.local");
        manager = login("manager@test.local");
        admin = login("admin@test.local");
    }

    @Test
    void businessUserOnlySeesOwnBusinesses() throws Exception {
        mvc.perform(get("/api/businesses").cookie(pepe)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(mine.getId().toString()));
        mvc.perform(get("/api/businesses").cookie(admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void readingAnotherBusinessIsForbiddenEverywhere() throws Exception {
        String o = other.getId().toString();
        mvc.perform(get("/api/businesses/" + o).cookie(pepe)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/businesses/" + o + "/devices").cookie(pepe)).andExpect(status().isForbidden());
        mvc.perform(get("/api/businesses/" + o + "/members").cookie(pepe)).andExpect(status().isForbidden());
        mvc.perform(get("/api/businesses/" + o + "/analytics/summary").cookie(pepe)).andExpect(status().isForbidden());
        mvc.perform(get("/api/businesses/" + o + "/analytics/timeline").cookie(pepe)).andExpect(status().isForbidden());
        mvc.perform(get("/api/businesses/" + o + "/analytics/devices").cookie(pepe)).andExpect(status().isForbidden());
        mvc.perform(get("/api/businesses/" + o + "/analytics/recent").cookie(pepe)).andExpect(status().isForbidden());
        mvc.perform(get("/api/businesses/" + o + "/analytics/export.csv").cookie(pepe)
                .param("from", "2026-01-01").param("to", "2026-01-31")).andExpect(status().isForbidden());
        mvc.perform(get("/api/devices/" + otherDevice.getId()).cookie(pepe)).andExpect(status().isForbidden());
        mvc.perform(get("/api/devices/" + otherDevice.getId() + "/qr.png").cookie(pepe)).andExpect(status().isForbidden());
        mvc.perform(get("/api/devices/" + otherDevice.getId() + "/qr.svg").cookie(pepe)).andExpect(status().isForbidden());
    }

    @Test
    void ownBusinessIsReadable() throws Exception {
        String m = mine.getId().toString();
        mvc.perform(get("/api/businesses/" + m).cookie(pepe)).andExpect(status().isOk());
        mvc.perform(get("/api/businesses/" + m + "/devices").cookie(pepe)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nfcUrl").value("https://r.test.local/d/" + myDevice.getPublicCode() + "?src=nfc"))
                .andExpect(jsonPath("$[0].qrUrl").value("https://r.test.local/d/" + myDevice.getPublicCode() + "?src=qr"));
        mvc.perform(get("/api/businesses/" + m + "/analytics/summary").cookie(pepe)).andExpect(status().isOk());
        mvc.perform(get("/api/devices/" + myDevice.getId()).cookie(pepe)).andExpect(status().isOk());
        mvc.perform(get("/api/devices/" + myDevice.getId() + "/qr.png").cookie(manager)).andExpect(status().isOk());
    }

    @Test
    void adminCanReadAnyBusiness() throws Exception {
        mvc.perform(get("/api/businesses/" + other.getId()).cookie(admin)).andExpect(status().isOk());
        mvc.perform(get("/api/devices/" + otherDevice.getId()).cookie(admin)).andExpect(status().isOk());
        mvc.perform(get("/api/businesses/" + other.getId() + "/analytics/summary").cookie(admin)).andExpect(status().isOk());
    }

    @Test
    void mutationsAreAdminOnly() throws Exception {
        String m = mine.getId().toString();
        String body = "{\"name\":\"X\",\"type\":\"QR\"}";
        mvc.perform(post("/api/businesses/" + m + "/devices").cookie(pepe).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/businesses").cookie(pepe).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Nuevo\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/businesses/" + m).cookie(pepe).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hack\",\"googleReviewUrl\":\"https://evil.example\",\"timezone\":\"Europe/Madrid\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/businesses/" + m + "/status").cookie(pepe).contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/devices/" + myDevice.getId() + "/status").cookie(pepe)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").cookie(pepe)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/devices").cookie(pepe)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/analytics/summary").cookie(pepe)).andExpect(status().isForbidden());

        mvc.perform(post("/api/businesses/" + m + "/devices").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.publicCode").isNotEmpty());
    }

    @Test
    void ownerCanEditProfileButManagerCannot() throws Exception {
        String m = mine.getId().toString();
        String body = "{\"name\":\"Barbería Pepe & Hijos\",\"timezone\":\"Europe/Madrid\"}";
        mvc.perform(patch("/api/businesses/" + m + "/profile").cookie(manager).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/businesses/" + m + "/profile").cookie(pepe).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Barbería Pepe & Hijos"));
        mvc.perform(patch("/api/businesses/" + other.getId() + "/profile").cookie(pepe)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    }

    @Test
    void googleReviewUrlMustBeHttps() throws Exception {
        String m = mine.getId().toString();
        mvc.perform(put("/api/businesses/" + m).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\",\"googleReviewUrl\":\"http://insecure.example/r\",\"timezone\":\"Europe/Madrid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.googleReviewUrl").exists());
        mvc.perform(put("/api/businesses/" + m).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B\",\"googleReviewUrl\":\"https://g.page/r/OK/review\",\"timezone\":\"Europe/Madrid\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminCreatesBusinessWithOwnerInOneStep() throws Exception {
        mvc.perform(post("/api/businesses").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Barbería Nueva\",\"googleReviewUrl\":\"https://g.page/r/N/review\","
                                + "\"ownerEmail\":\"nuevo@test.local\",\"ownerFirstName\":\"Nuevo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.business.slug").value("barberia-nueva"))
                .andExpect(jsonPath("$.owner.created").value(true))
                .andExpect(jsonPath("$.owner.generatedPassword").isNotEmpty());
    }
}
