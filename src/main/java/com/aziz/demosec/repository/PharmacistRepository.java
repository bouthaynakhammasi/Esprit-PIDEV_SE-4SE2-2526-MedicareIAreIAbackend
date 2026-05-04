package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.Pharmacist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacistRepository extends JpaRepository<Pharmacist, Long> {

    List<Pharmacist> findByPharmacy_Id(Long pharmacyId);

    Optional<Pharmacist> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Pharmacist> findByStatus(com.aziz.demosec.Entities.PharmacistStatus status);
}
