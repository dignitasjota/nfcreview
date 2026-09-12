package com.reviewtap.business;

import com.reviewtap.auth.AuthPrincipal;
import com.reviewtap.business.BusinessDtos.BusinessCreateRequest;
import com.reviewtap.business.BusinessDtos.BusinessCreatedResponse;
import com.reviewtap.business.BusinessDtos.BusinessProfileUpdateRequest;
import com.reviewtap.business.BusinessDtos.BusinessResponse;
import com.reviewtap.business.BusinessDtos.BusinessUpdateRequest;
import com.reviewtap.business.BusinessDtos.MemberAddRequest;
import com.reviewtap.business.BusinessDtos.MemberAddedResponse;
import com.reviewtap.business.BusinessDtos.MemberResponse;
import com.reviewtap.business.BusinessDtos.OwnerResult;
import com.reviewtap.common.ApiException;
import com.reviewtap.common.Passwords;
import com.reviewtap.common.Slugs;
import com.reviewtap.device.DeviceRepository;
import com.reviewtap.user.User;
import com.reviewtap.user.UserRepository;
import com.reviewtap.user.UserRole;
import com.reviewtap.user.UserService;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businesses;
    private final BusinessUserRepository businessUsers;
    private final UserRepository users;
    private final DeviceRepository devices;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<BusinessResponse> listFor(AuthPrincipal principal) {
        List<Business> list = principal.isAdmin() ? businesses.findAllOrdered()
                : businesses.findAllByUserId(principal.userId());
        Map<UUID, Long> counts = devices.countGroupedByBusiness().stream()
                .collect(Collectors.toMap(DeviceRepository.BusinessDeviceCount::getBusinessId,
                        DeviceRepository.BusinessDeviceCount::getTotal));
        return list.stream().map(b -> toResponse(b, counts.getOrDefault(b.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public BusinessResponse get(UUID id) {
        Business b = find(id);
        return toResponse(b, devices.countByBusinessId(id));
    }

    @Transactional(readOnly = true)
    public Business find(UUID id) {
        return businesses.findById(id).orElseThrow(() -> ApiException.notFound("Negocio"));
    }

    @Transactional
    public BusinessCreatedResponse create(BusinessCreateRequest req) {
        Business b = new Business();
        b.setName(req.name().trim());
        b.setSlug(uniqueSlug(req.name()));
        b.setGoogleReviewUrl(blankToNull(req.googleReviewUrl()));
        b.setAddress(blankToNull(req.address()));
        b.setPhone(blankToNull(req.phone()));
        b.setTimezone(validTimezone(req.timezone()));
        b.setActive(true);
        b = businesses.save(b);
        log.info("Negocio {} creado ({})", b.getId(), b.getSlug());

        OwnerResult owner = null;
        if (req.ownerEmail() != null && !req.ownerEmail().isBlank()) {
            MemberAddedResponse added = addMember(b, new MemberAddRequest(req.ownerEmail(), BusinessRole.OWNER,
                    req.ownerFirstName(), req.ownerLastName(), req.ownerPassword()));
            owner = new OwnerResult(added.member().userId(), added.member().email(), added.userCreated(),
                    added.generatedPassword());
        }
        return new BusinessCreatedResponse(toResponse(b, 0), owner);
    }

    @Transactional
    public BusinessResponse update(UUID id, BusinessUpdateRequest req) {
        Business b = find(id);
        b.setName(req.name().trim());
        b.setGoogleReviewUrl(blankToNull(req.googleReviewUrl()));
        b.setLogoUrl(blankToNull(req.logoUrl()));
        b.setAddress(blankToNull(req.address()));
        b.setPhone(blankToNull(req.phone()));
        b.setTimezone(validTimezone(req.timezone()));
        log.info("Negocio {} actualizado por un administrador", id);
        return get(id);
    }

    @Transactional
    public BusinessResponse updateProfile(UUID id, BusinessProfileUpdateRequest req) {
        Business b = find(id);
        b.setName(req.name().trim());
        b.setLogoUrl(blankToNull(req.logoUrl()));
        b.setAddress(blankToNull(req.address()));
        b.setPhone(blankToNull(req.phone()));
        b.setTimezone(validTimezone(req.timezone()));
        return get(id);
    }

    @Transactional
    public BusinessResponse setActive(UUID id, boolean active) {
        Business b = find(id);
        b.setActive(active);
        log.info("Negocio {} {}", id, active ? "activado" : "desactivado");
        return get(id);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> members(UUID businessId) {
        find(businessId);
        return businessUsers.findAllByBusinessIdOrderByCreatedAtAsc(businessId).stream()
                .map(BusinessService::toMember).toList();
    }

    @Transactional
    public MemberAddedResponse addMember(UUID businessId, MemberAddRequest req) {
        return addMember(find(businessId), req);
    }

    private MemberAddedResponse addMember(Business business, MemberAddRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = users.findByEmailIgnoreCase(email).orElse(null);
        boolean created = false;
        String generated = null;
        if (user == null) {
            String password = req.password();
            if (password == null || password.isBlank()) {
                password = Passwords.generate();
                generated = password;
            }
            String firstName = req.firstName() == null || req.firstName().isBlank() ? "Propietario" : req.firstName();
            user = userService.createUser(email, password, firstName, req.lastName(), UserRole.BUSINESS_USER);
            created = true;
        } else if (user.getRole() == UserRole.ADMIN) {
            throw ApiException.badRequest("ADMIN_NOT_ASSIGNABLE", "Un administrador ya tiene acceso a todos los negocios");
        }
        if (businessUsers.existsByUserIdAndBusinessId(user.getId(), business.getId())) {
            throw ApiException.conflict("ALREADY_MEMBER", "Ese usuario ya pertenece al negocio");
        }
        BusinessUser bu = businessUsers.save(new BusinessUser(user, business, req.role()));
        log.info("Usuario {} asignado al negocio {} como {}", user.getId(), business.getId(), req.role());
        return new MemberAddedResponse(toMember(bu), created, generated);
    }

    @Transactional
    public void removeMember(UUID businessId, UUID userId) {
        BusinessUser bu = businessUsers.findByUserIdAndBusinessId(userId, businessId)
                .orElseThrow(() -> ApiException.notFound("Miembro"));
        businessUsers.delete(bu);
        log.info("Usuario {} desvinculado del negocio {}", userId, businessId);
    }

    private String uniqueSlug(String name) {
        String base = Slugs.from(name);
        String candidate = base;
        int i = 2;
        while (businesses.existsBySlug(candidate)) {
            candidate = base + "-" + i++;
        }
        return candidate;
    }

    static String validTimezone(String tz) {
        if (tz == null || tz.isBlank()) {
            return Business.DEFAULT_TIMEZONE;
        }
        if (!ZoneId.getAvailableZoneIds().contains(tz.trim())) {
            throw ApiException.badRequest("INVALID_TIMEZONE", "Zona horaria no válida");
        }
        return tz.trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static BusinessResponse toResponse(Business b, long deviceCount) {
        return new BusinessResponse(b.getId(), b.getName(), b.getSlug(), b.getGoogleReviewUrl(), b.getLogoUrl(),
                b.getAddress(), b.getPhone(), b.getTimezone(), b.isActive(), b.getCreatedAt(), deviceCount);
    }

    private static MemberResponse toMember(BusinessUser bu) {
        User u = bu.getUser();
        return new MemberResponse(u.getId(), u.getEmail(), u.fullName(), u.isEnabled(), bu.getRole(),
                bu.getCreatedAt());
    }
}
