package com.reviewtap.device;

import com.reviewtap.business.Business;
import com.reviewtap.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un soporte físico (tarjeta, placa, pegatina) con chip NFC y/o código QR. */
@Entity
@Table(name = "device")
@Getter
@Setter
@NoArgsConstructor
public class Device extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @Column(name = "public_code", nullable = false, length = 32)
    private String publicCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "location_description", length = 200)
    private String locationDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceType type;

    @Column(nullable = false)
    private boolean active = true;
}
