package com.reviewtap.seed;

import com.reviewtap.business.Business;
import com.reviewtap.business.BusinessRepository;
import com.reviewtap.business.BusinessRole;
import com.reviewtap.business.BusinessUser;
import com.reviewtap.business.BusinessUserRepository;
import com.reviewtap.config.AppProperties;
import com.reviewtap.device.Device;
import com.reviewtap.device.DeviceRepository;
import com.reviewtap.device.DeviceType;
import com.reviewtap.device.PublicCodeGenerator;
import com.reviewtap.interaction.Interaction;
import com.reviewtap.interaction.InteractionRepository;
import com.reviewtap.interaction.InteractionType;
import com.reviewtap.interaction.UserAgentCategory;
import com.reviewtap.user.User;
import com.reviewtap.user.UserRepository;
import com.reviewtap.user.UserRole;
import com.reviewtap.user.UserService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de demostración (sólo con {@code app.seed.enabled=true}, activo en el perfil dev).
 * Idempotente: si ya existe el negocio demo no hace nada. Genera ~60 días de interacciones con
 * patrón semanal para que los gráficos tengan forma.
 */
@Slf4j
@Component
@Order(2)
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    public static final String ADMIN_EMAIL = "admin@example.local";
    public static final String OWNER_EMAIL = "pepe@example.local";
    public static final String DEMO_SLUG = "barberia-demo";

    private final AppProperties props;
    private final UserRepository users;
    private final UserService userService;
    private final BusinessRepository businesses;
    private final BusinessUserRepository businessUsers;
    private final DeviceRepository devices;
    private final InteractionRepository interactions;
    private final PublicCodeGenerator codes;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (businesses.existsBySlug(DEMO_SLUG)) {
            return;
        }
        String password = props.seed().password() == null || props.seed().password().isBlank()
                ? "Demo1234!" : props.seed().password();

        if (!users.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            userService.createUser(ADMIN_EMAIL, password, "Admin", "Demo", UserRole.ADMIN);
        }
        User owner = users.findByEmailIgnoreCase(OWNER_EMAIL)
                .orElseGet(() -> userService.createUser(OWNER_EMAIL, password, "Pepe", "García",
                        UserRole.BUSINESS_USER));

        Business demo = new Business();
        demo.setName("Barbería Demo");
        demo.setSlug(DEMO_SLUG);
        demo.setGoogleReviewUrl("https://search.google.com/local/writereview?placeid=ChIJDemoPlaceIdBarberia");
        demo.setAddress("Calle Mayor 12, Madrid");
        demo.setPhone("+34 600 000 000");
        demo.setTimezone("Europe/Madrid");
        demo = businesses.save(demo);
        businessUsers.save(new BusinessUser(owner, demo, BusinessRole.OWNER));

        Device counter = device(demo, "Mostrador principal", "Recepción", DeviceType.NFC_QR);
        Device entrance = device(demo, "Entrada", "Puerta principal", DeviceType.QR);
        Device card = device(demo, "Tarjeta empleado", "Empleado 1", DeviceType.NFC);

        Random random = new Random(42);
        List<Interaction> batch = new ArrayList<>();
        ZoneId zone = demo.zoneId();
        LocalDate today = LocalDate.now(zone);
        for (int daysAgo = 60; daysAgo >= 0; daysAgo--) {
            LocalDate day = today.minusDays(daysAgo);
            double weekday = switch (day.getDayOfWeek()) {
                case SATURDAY -> 1.4;
                case SUNDAY -> 0.3;
                case FRIDAY -> 1.2;
                default -> 1.0;
            };
            addDay(batch, counter, day, zone, random, (int) Math.round((4 + random.nextInt(4)) * weekday), 0.75);
            addDay(batch, entrance, day, zone, random, (int) Math.round((2 + random.nextInt(3)) * weekday), 0.0);
            addDay(batch, card, day, zone, random, (int) Math.round((1 + random.nextInt(2)) * weekday), 1.0);
        }
        interactions.saveAll(batch);

        // Segundo negocio sin datos: útil para verificar el aislamiento entre negocios.
        Business other = new Business();
        other.setName("Cafetería Aroma");
        other.setSlug("cafeteria-aroma");
        other.setGoogleReviewUrl("https://g.page/r/CafeteriaAromaDemo/review");
        other = businesses.save(other);
        User otherOwner = users.findByEmailIgnoreCase("ana@example.local")
                .orElseGet(() -> userService.createUser("ana@example.local", password, "Ana", "López",
                        UserRole.BUSINESS_USER));
        businessUsers.save(new BusinessUser(otherOwner, other, BusinessRole.OWNER));
        device(other, "Barra", "Barra principal", DeviceType.NFC_QR);

        log.info("Seed demo creado: {} interacciones. Credenciales: {} / {} (admin), {} / {} (negocio)",
                batch.size(), ADMIN_EMAIL, password, OWNER_EMAIL, password);
    }

    private Device device(Business business, String name, String location, DeviceType type) {
        Device d = new Device();
        d.setBusiness(business);
        d.setPublicCode(codes.generate());
        d.setName(name);
        d.setLocationDescription(location);
        d.setType(type);
        d.setActive(true);
        return devices.save(d);
    }

    /** Sólo en horario comercial (10–20 h); {@code nfcRatio} = proporción de accesos NFC frente a QR. */
    private static void addDay(List<Interaction> out, Device device, LocalDate day, ZoneId zone, Random random,
            int count, double nfcRatio) {
        LocalDate today = LocalDate.now(zone);
        for (int i = 0; i < count; i++) {
            int hour = 10 + random.nextInt(10);
            LocalTime time = LocalTime.of(hour, random.nextInt(60), random.nextInt(60));
            Instant at = day.atTime(time).atZone(zone).toInstant();
            if (day.equals(today) && at.isAfter(Instant.now())) {
                continue;
            }
            InteractionType type = random.nextDouble() < nfcRatio ? InteractionType.NFC : InteractionType.QR;
            UserAgentCategory ua = random.nextDouble() < 0.9 ? UserAgentCategory.MOBILE : UserAgentCategory.DESKTOP;
            out.add(new Interaction(device.getId(), at, type, ua, null));
        }
    }
}
