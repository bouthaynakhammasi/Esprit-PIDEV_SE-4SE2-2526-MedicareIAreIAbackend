package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {
    boolean existsByNameIgnoreCase(String name);
}