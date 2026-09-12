package com.reviewtap.user;

import com.reviewtap.auth.CurrentUser;
import com.reviewtap.user.UserDtos.ResetPasswordRequest;
import com.reviewtap.user.UserDtos.UserCreateRequest;
import com.reviewtap.user.UserDtos.UserCreatedResponse;
import com.reviewtap.user.UserDtos.UserResponse;
import com.reviewtap.user.UserDtos.UserStatusRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestión de usuarios. Toda la ruta /api/admin/** exige ROLE_ADMIN (SecurityConfig). */
@Tag(name = "Admin · Usuarios")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return userService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserCreatedResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @PatchMapping("/{id}/status")
    public UserResponse setStatus(@PathVariable UUID id, @Valid @RequestBody UserStatusRequest request) {
        return userService.setEnabled(id, request.enabled(), CurrentUser.require().userId());
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(id, req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
