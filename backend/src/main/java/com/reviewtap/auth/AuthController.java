package com.reviewtap.auth;

import com.reviewtap.auth.AuthDtos.ChangePasswordRequest;
import com.reviewtap.auth.AuthDtos.ForgotPasswordRequest;
import com.reviewtap.auth.AuthDtos.ResetPasswordRequest;
import com.reviewtap.auth.AuthDtos.LoginRequest;
import com.reviewtap.auth.AuthDtos.MeResponse;
import com.reviewtap.interaction.ClientKeyResolver;
import com.reviewtap.user.User;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final SessionCookies cookies;
    private final PasswordResetService passwordReset;

    @PostMapping("/login")
    public ResponseEntity<MeResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        User user = authService.authenticate(request.email(), request.password(), ClientKeyResolver.clientIp(http));
        String token = jwtService.issue(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.create(token, jwtService.ttlSeconds()).toString())
                .body(authService.me(user.getId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        CurrentUser.find().ifPresent(p -> authService.logout(p.userId()));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }

    @GetMapping("/me")
    public MeResponse me() {
        return authService.me(CurrentUser.require().userId());
    }

    /** Responde 204 exista o no el email (evita enumerar cuentas). */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest http) {
        passwordReset.request(request.email(), ClientKeyResolver.clientIp(http));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordReset.reset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.require().userId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
