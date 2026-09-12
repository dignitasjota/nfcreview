package com.reviewtap.interaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Acceso a un dispositivo. Anónimo por diseño: sin IP, sin fingerprint, sin UA completo.
 * Se guarda el id del dispositivo (no la entidad) para que el insert no cargue nada.
 */
@Entity
@Table(name = "interaction")
@Getter
@Setter
@NoArgsConstructor
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 20)
    private InteractionType interactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_agent_category", length = 20)
    private UserAgentCategory userAgentCategory;

    @Column(length = 255)
    private String referer;

    public Interaction(UUID deviceId, Instant createdAt, InteractionType type, UserAgentCategory uaCategory,
            String referer) {
        this.deviceId = deviceId;
        this.createdAt = createdAt;
        this.interactionType = type;
        this.userAgentCategory = uaCategory;
        this.referer = referer;
    }
}
