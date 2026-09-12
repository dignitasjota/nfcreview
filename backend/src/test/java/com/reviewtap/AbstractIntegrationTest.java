package com.reviewtap;

import com.reviewtap.business.Business;
import com.reviewtap.business.BusinessRepository;
import com.reviewtap.business.BusinessRole;
import com.reviewtap.business.BusinessUser;
import com.reviewtap.business.BusinessUserRepository;
import com.reviewtap.device.Device;
import com.reviewtap.device.DeviceRepository;
import com.reviewtap.device.DeviceType;
import com.reviewtap.device.PublicCodeGenerator;
import com.reviewtap.interaction.Interaction;
import com.reviewtap.interaction.InteractionGuard;
import com.reviewtap.interaction.InteractionRepository;
import com.reviewtap.interaction.InteractionType;
import com.reviewtap.interaction.UserAgentCategory;
import com.reviewtap.user.User;
import com.reviewtap.user.UserRepository;
import com.reviewtap.user.UserRole;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Base de los tests de integración: PostgreSQL real (Testcontainers), MockMvc y helpers de datos. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    public static final String PASSWORD = "Secret123!";

    @Autowired protected MockMvc mvc;
    @Autowired protected UserRepository users;
    @Autowired protected BusinessRepository businesses;
    @Autowired protected BusinessUserRepository businessUsers;
    @Autowired protected DeviceRepository devices;
    @Autowired protected InteractionRepository interactions;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected PublicCodeGenerator codes;
    @Autowired protected InteractionGuard guard;

    @BeforeEach
    void cleanDatabase() {
        guard.reset();
        interactions.deleteAll();
        devices.deleteAll();
        businessUsers.deleteAll();
        businesses.deleteAll();
        users.deleteAll();
    }

    protected User user(String email, UserRole role) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(PASSWORD));
        u.setFirstName("Test");
        u.setLastName("User");
        u.setRole(role);
        u.setEnabled(true);
        return users.save(u);
    }

    protected Business business(String name, String url) {
        Business b = new Business();
        b.setName(name);
        b.setSlug(name.toLowerCase().replace(' ', '-') + "-" + UUID.randomUUID().toString().substring(0, 6));
        b.setGoogleReviewUrl(url);
        b.setTimezone("Europe/Madrid");
        b.setActive(true);
        return businesses.save(b);
    }

    protected void member(User u, Business b, BusinessRole role) {
        businessUsers.save(new BusinessUser(u, b, role));
    }

    protected Device device(Business b, String name) {
        Device d = new Device();
        d.setBusiness(b);
        d.setPublicCode(codes.generate());
        d.setName(name);
        d.setLocationDescription("Ubicación " + name);
        d.setType(DeviceType.NFC_QR);
        d.setActive(true);
        return devices.save(d);
    }

    protected Interaction interaction(Device d, InteractionType type, Instant at) {
        return interactions.save(new Interaction(d.getId(), at, type, UserAgentCategory.MOBILE, null));
    }

    protected Cookie login(String email) throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn();
        if (result.getResponse().getStatus() != 200) {
            throw new AssertionError("Login falló: " + result.getResponse().getContentAsString());
        }
        Cookie cookie = result.getResponse().getCookie("rt_session");
        if (cookie == null) {
            throw new AssertionError("Sin cookie de sesión");
        }
        return cookie;
    }
}
