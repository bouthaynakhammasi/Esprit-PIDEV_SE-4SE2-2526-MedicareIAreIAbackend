package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceProviderRepository extends JpaRepository<ServiceProvider, Long> {
    Optional<ServiceProvider> findByUser_Id(Long userId);
    Optional<ServiceProvider> findByUser_Email(String email);
    List<ServiceProvider> findByVerifiedTrue();
    List<ServiceProvider> findByVerifiedFalse();

    @Query("SELECT sp FROM ServiceProvider sp JOIN sp.specialties s WHERE s.id = :serviceId AND sp.verified = true")
    List<ServiceProvider> findVerifiedByServiceId(@Param("serviceId") Long serviceId);

    @Query("SELECT sp FROM ServiceProvider sp JOIN sp.specialties s WHERE s.id = :serviceId AND sp.verified = true ORDER BY sp.averageRating DESC")
    List<ServiceProvider> findVerifiedByServiceIdOrderByRatingDesc(@Param("serviceId") Long serviceId);

    @Query("SELECT sp FROM ServiceProvider sp JOIN sp.specialties s WHERE s.id = :serviceId AND sp.verified = true AND sp.averageRating >= :minRating ORDER BY sp.averageRating DESC")
    List<ServiceProvider> findVerifiedByServiceIdAndMinRating(@Param("serviceId") Long serviceId, @Param("minRating") double minRating);
}
