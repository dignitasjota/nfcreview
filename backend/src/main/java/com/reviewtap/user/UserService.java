package com.reviewtap.user;

import com.reviewtap.business.BusinessUser;
import com.reviewtap.business.BusinessUserRepository;
import com.reviewtap.common.ApiException;
import com.reviewtap.common.Passwords;
import com.reviewtap.user.UserDtos.UserBusinessRef;
import com.reviewtap.user.UserDtos.UserCreateRequest;
import com.reviewtap.user.UserDtos.UserCreatedResponse;
import com.reviewtap.user.UserDtos.UserResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final BusinessUserRepository businessUsers;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        List<User> all = users.findAllOrdered();
        Map<UUID, List<UserBusinessRef>> refs = businessUsers.findAll().stream()
                .collect(Collectors.groupingBy(bu -> bu.getUser().getId(), Collectors.mapping(
                        bu -> new UserBusinessRef(bu.getBusiness().getId(), bu.getBusiness().getName(), bu.getRole()),
                        Collectors.toList())));
        return all.stream().map(u -> toResponse(u, refs.getOrDefault(u.getId(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("Usuario"));
        return toResponse(user, refsOf(id));
    }

    @Transactional
    public UserCreatedResponse create(UserCreateRequest req) {
        String password = req.password() == null || req.password().isBlank() ? Passwords.generate() : null;
        User user = createUser(req.email(), password != null ? password : req.password(), req.firstName(),
                req.lastName(), req.role());
        log.info("Usuario {} creado con rol {}", user.getId(), user.getRole());
        return new UserCreatedResponse(toResponse(user, List.of()), password);
    }

    /** Crea el usuario si no existe. Devuelve la contraseña generada sólo si se ha creado sin contraseña dada. */
    @Transactional
    public User createUser(String email, String rawPassword, String firstName, String lastName, UserRole role) {
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_IN_USE", "Ya existe un usuario con ese email");
        }
        User user = new User();
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName.trim());
        user.setLastName(lastName == null ? "" : lastName.trim());
        user.setRole(role);
        user.setEnabled(true);
        return users.save(user);
    }

    @Transactional
    public UserResponse setEnabled(UUID id, boolean enabled, UUID actingUserId) {
        if (id.equals(actingUserId) && !enabled) {
            throw ApiException.badRequest("SELF_DISABLE", "No puedes deshabilitar tu propio usuario");
        }
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("Usuario"));
        user.setEnabled(enabled);
        log.info("Usuario {} {}", id, enabled ? "habilitado" : "deshabilitado");
        return toResponse(user, refsOf(id));
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("Usuario"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        log.info("Contraseña restablecida por un administrador para el usuario {}", id);
    }

    private List<UserBusinessRef> refsOf(UUID userId) {
        return businessUsers.findAllByUserId(userId).stream()
                .map(bu -> new UserBusinessRef(bu.getBusiness().getId(), bu.getBusiness().getName(), bu.getRole()))
                .toList();
    }

    static UserResponse toResponse(User u, List<UserBusinessRef> businesses) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getRole(),
                u.isEnabled(), u.getCreatedAt(), businesses);
    }

    static UserBusinessRef ref(BusinessUser bu) {
        return new UserBusinessRef(bu.getBusiness().getId(), bu.getBusiness().getName(), bu.getRole());
    }
}
