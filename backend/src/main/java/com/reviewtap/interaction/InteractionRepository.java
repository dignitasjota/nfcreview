package com.reviewtap.interaction;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, UUID> {

    long countByDeviceId(UUID deviceId);
}
