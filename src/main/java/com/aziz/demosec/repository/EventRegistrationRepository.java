package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.EventRegistration;
import com.aziz.demosec.Entities.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    boolean existsByEventIdAndParticipantId(Long eventId, Long participantId);

    Optional<EventRegistration> findByEventIdAndParticipantId(Long eventId, Long participantId);

    List<EventRegistration> findByEventIdOrderByCreatedAtDesc(Long eventId);

    List<EventRegistration> findByParticipantIdOrderByCreatedAtDesc(Long participantId);

    List<EventRegistration> findByEventIdAndStatus(Long eventId, RegistrationStatus status);

    List<EventRegistration> findByEventIdAndStatusOrderByCreatedAtAsc(Long eventId, RegistrationStatus status);
}
