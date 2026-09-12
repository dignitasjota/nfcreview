package com.reviewtap.auth;

import com.reviewtap.business.BusinessRole;
import com.reviewtap.business.BusinessUserRepository;
import com.reviewtap.device.DeviceRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Autorización a nivel de recurso (multi-tenant). Se usa desde {@code @PreAuthorize("@access...")}
 * en cada endpoint que recibe un businessId o deviceId. ADMIN accede a todo; un BUSINESS_USER sólo
 * a los negocios de los que es miembro.
 */
@Component("access")
@RequiredArgsConstructor
public class BusinessAccess {

    private final BusinessUserRepository businessUsers;
    private final DeviceRepository devices;

    public boolean canRead(UUID businessId) {
        AuthPrincipal user = CurrentUser.require();
        return user.isAdmin() || businessUsers.existsByUserIdAndBusinessId(user.userId(), businessId);
    }

    /** OWNER del negocio o ADMIN: edición del perfil del negocio. */
    public boolean canManage(UUID businessId) {
        AuthPrincipal user = CurrentUser.require();
        if (user.isAdmin()) {
            return true;
        }
        return businessUsers.findByUserIdAndBusinessId(user.userId(), businessId)
                .map(bu -> bu.getRole() == BusinessRole.OWNER)
                .orElse(false);
    }

    public boolean canReadDevice(UUID deviceId) {
        AuthPrincipal user = CurrentUser.require();
        if (user.isAdmin()) {
            return true;
        }
        return devices.findById(deviceId)
                .map(d -> businessUsers.existsByUserIdAndBusinessId(user.userId(), d.getBusiness().getId()))
                .orElse(false);
    }
}
