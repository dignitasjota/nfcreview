package com.reviewtap.auth;

import com.reviewtap.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie de sesión: HttpOnly (inaccesible desde JS), SameSite=Strict (no se envía en peticiones
 * cross-site, lo que neutraliza CSRF clásico) y Secure salvo en desarrollo local.
 */
@Component
public class SessionCookies {

    private final AppProperties.Security security;

    public SessionCookies(AppProperties props) {
        this.security = props.security();
    }

    public String name() {
        return security.cookieName();
    }

    public ResponseCookie create(String token, long maxAgeSeconds) {
        return base(token).maxAge(maxAgeSeconds).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> security.cookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(security.cookieName(), value)
                .httpOnly(true)
                .secure(security.cookieSecure())
                .sameSite("Strict")
                .path("/");
    }
}
