package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.EventParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventParticipationRepository extends JpaRepository<EventParticipation, Long> {

    boolean existsByEventIdAndPatientId(Long eventId, Long patientId);

    void deleteByEventIdAndPatientId(Long eventId, Long patientId);

    Optional<EventParticipation> findByEventIdAndPatientId(Long eventId, Long patientId);

    long countByEventId(Long eventId);
    
    long countByEventIdAndAttendedTrue(Long eventId);

    java.util.List<EventParticipation> findByEventId(Long eventId);
}
