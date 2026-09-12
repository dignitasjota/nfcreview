package com.reviewtap.business;

import com.reviewtap.auth.CurrentUser;
import com.reviewtap.business.BusinessDtos.BusinessCreateRequest;
import com.reviewtap.business.BusinessDtos.BusinessCreatedResponse;
import com.reviewtap.business.BusinessDtos.BusinessProfileUpdateRequest;
import com.reviewtap.business.BusinessDtos.BusinessResponse;
import com.reviewtap.business.BusinessDtos.BusinessUpdateRequest;
import com.reviewtap.business.BusinessDtos.MemberAddRequest;
import com.reviewtap.business.BusinessDtos.MemberAddedResponse;
import com.reviewtap.business.BusinessDtos.MemberResponse;
import com.reviewtap.business.BusinessDtos.StatusRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Negocios. Lectura para miembros y ADMIN; creación, URL de Google, estado y miembros sólo ADMIN;
 * perfil básico también para el OWNER.
 */
@Tag(name = "Negocios")
@RestController
@RequestMapping("/api/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @GetMapping
    public List<BusinessResponse> list() {
        return businessService.listFor(CurrentUser.require());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessCreatedResponse create(@Valid @RequestBody BusinessCreateRequest request) {
        return businessService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.canRead(#id)")
    public BusinessResponse get(@PathVariable UUID id) {
        return businessService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BusinessResponse update(@PathVariable UUID id, @Valid @RequestBody BusinessUpdateRequest request) {
        return businessService.update(id, request);
    }

    @PatchMapping("/{id}/profile")
    @PreAuthorize("@access.canManage(#id)")
    public BusinessResponse updateProfile(@PathVariable UUID id,
            @Valid @RequestBody BusinessProfileUpdateRequest request) {
        return businessService.updateProfile(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public BusinessResponse setStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        return businessService.setActive(id, request.active());
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("@access.canRead(#id)")
    public List<MemberResponse> members(@PathVariable UUID id) {
        return businessService.members(id);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberAddedResponse addMember(@PathVariable UUID id, @Valid @RequestBody MemberAddRequest request) {
        return businessService.addMember(id, request);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        businessService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }
}
