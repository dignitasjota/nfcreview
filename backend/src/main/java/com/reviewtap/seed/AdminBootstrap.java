package com.reviewtap.seed;

import com.reviewtap.config.AppProperties;
import com.reviewtap.user.UserRepository;
import com.reviewtap.user.UserRole;
import com.reviewtap.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Crea el primer ADMIN en producción a partir de APP_BOOTSTRAP_ADMIN_EMAIL / APP_BOOTSTRAP_ADMIN_PASSWORD,
 * sólo si todavía no existe ningún administrador. Idempotente: en arranques posteriores no hace nada.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final AppProperties props;
    private final UserRepository users;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(UserRole.ADMIN)) {
            return;
        }
        AppProperties.Bootstrap cfg = props.bootstrap();
        if (cfg == null || isBlank(cfg.adminEmail()) || isBlank(cfg.adminPassword())) {
            if (props.seed() != null && props.seed().enabled()) {
                return; // el seed de desarrollo crea su propio admin
            }
            log.warn("No existe ningún ADMIN y no se han definido APP_BOOTSTRAP_ADMIN_EMAIL / "
                    + "APP_BOOTSTRAP_ADMIN_PASSWORD. Nadie podrá entrar en el panel hasta crear uno.");
            return;
        }
        var admin = userService.createUser(cfg.adminEmail(), cfg.adminPassword(), "Admin", "", UserRole.ADMIN);
        log.info("ADMIN inicial creado: {}", admin.getId());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
