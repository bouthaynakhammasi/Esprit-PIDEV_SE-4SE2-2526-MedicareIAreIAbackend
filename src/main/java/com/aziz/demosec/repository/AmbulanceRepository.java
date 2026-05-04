package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {
    List<Ambulance> findByClinicId(Long clinicId);
    List<Ambulance> findByStatus(String status);
}
