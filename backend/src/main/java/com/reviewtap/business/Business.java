package com.reviewtap.business;

import com.reviewtap.common.AuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "business")
@Getter
@Setter
@NoArgsConstructor
public class Business extends AuditedEntity {

    public static final String DEFAULT_TIMEZONE = "Europe/Madrid";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 160)
    private String slug;

    @Column(name = "google_review_url", length = 2048)
    private String googleReviewUrl;

    @Column(name = "logo_url", length = 2048)
    private String logoUrl;

    @Column(length = 255)
    private String address;

    @Column(length = 40)
    private String phone;

    @Column(nullable = false, length = 64)
    private String timezone = DEFAULT_TIMEZONE;

    @Column(nullable = false)
    private boolean active = true;

    public ZoneId zoneId() {
        try {
            return ZoneId.of(timezone);
        } catch (RuntimeException e) {
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }
}
