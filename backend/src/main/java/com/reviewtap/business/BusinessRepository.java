package com.reviewtap.business;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByActiveTrue();

    @Query("select b from Business b order by b.name asc")
    List<Business> findAllOrdered();

    @Query("select bu.business from BusinessUser bu where bu.user.id = :userId order by bu.business.name asc")
    List<Business> findAllByUserId(@Param("userId") UUID userId);
}
