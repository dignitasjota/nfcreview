package com.reviewtap.business;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessUserRepository extends JpaRepository<BusinessUser, UUID> {

    boolean existsByUserIdAndBusinessId(UUID userId, UUID businessId);

    Optional<BusinessUser> findByUserIdAndBusinessId(UUID userId, UUID businessId);

    @EntityGraph(attributePaths = "user")
    List<BusinessUser> findAllByBusinessIdOrderByCreatedAtAsc(UUID businessId);

    @EntityGraph(attributePaths = "business")
    List<BusinessUser> findAllByUserId(UUID userId);
}
