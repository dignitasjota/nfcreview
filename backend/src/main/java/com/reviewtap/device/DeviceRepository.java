package com.reviewtap.device;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    @Query("""
            select new com.reviewtap.device.DeviceRedirectTarget(d.id, d.active, b.active, b.googleReviewUrl)
            from Device d join d.business b
            where d.publicCode = :code
            """)
    Optional<DeviceRedirectTarget> findRedirectTarget(@Param("code") String code);

    boolean existsByPublicCode(String publicCode);

    List<Device> findAllByBusinessIdOrderByCreatedAtAsc(UUID businessId);

    @EntityGraph(attributePaths = "business")
    @Query("select d from Device d order by d.createdAt desc")
    List<Device> findAllWithBusiness();

    @EntityGraph(attributePaths = "business")
    Optional<Device> findWithBusinessById(UUID id);

    interface BusinessDeviceCount {
        UUID getBusinessId();

        long getTotal();
    }

    @Query("select d.business.id as businessId, count(d) as total from Device d group by d.business.id")
    List<BusinessDeviceCount> countGroupedByBusiness();

    long countByBusinessId(UUID businessId);

    long countByActiveTrue();
}
