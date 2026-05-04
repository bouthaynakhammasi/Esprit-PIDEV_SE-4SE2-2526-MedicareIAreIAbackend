package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.Ambulance;
import com.aziz.demosec.Entities.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicRepository extends JpaRepository<Clinic, Long> {
    java.util.Optional<Clinic> findByUserId(Long userId);
}
