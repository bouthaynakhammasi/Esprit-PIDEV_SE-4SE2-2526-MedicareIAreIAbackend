package com.aziz.demosec.service;

import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.EventRegistrationCreateRequest;
import com.aziz.demosec.dto.EventRegistrationResponse;
import com.aziz.demosec.Entities.*;
import com.aziz.demosec.repository.EventRegistrationRepository;
import com.aziz.demosec.repository.MedicalEventRepository;
import com.aziz.demosec.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EventRegistrationServiceImpl implements IEventRegistrationService {

    private final EventRegistrationRepository eventRegistrationRepository;
    private final MedicalEventRepository medicalEventRepository;
    private final UserRepository userRepository;

    @Override
    public EventRegistrationResponse register(EventRegistrationCreateRequest request) {

        MedicalEvent event = medicalEventRepository.findById(request.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + request.getEventId()));

        User user = userRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getParticipantId()));

        if (eventRegistrationRepository.existsByEventIdAndParticipantId(event.getId(), user.getId())) {
            throw new IllegalStateException("Participant already registered for this event");
        }

        // optional: capacity check for PhysicalEvent
        if (event instanceof PhysicalEvent pe && pe.getCapacity() != null) {
            long registeredCount = eventRegistrationRepository.findByEventIdOrderByCreatedAtDesc(event.getId())
                    .stream().filter(r -> r.getStatus() == RegistrationStatus.REGISTERED || r.getStatus() == RegistrationStatus.VALIDATED)
                    .count();
            if (registeredCount >= pe.getCapacity()) {
                throw new IllegalStateException("Event capacity reached");
            }
        }

        EventRegistration reg = EventRegistration.builder()
                .event(event)
                .participant(user)
                .status(RegistrationStatus.REGISTERED)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(eventRegistrationRepository.save(reg));
    }

    @Override
    public EventRegistrationResponse validate(Long registrationId) {
        EventRegistration reg = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new EntityNotFoundException("Registration not found: " + registrationId));

        reg.setStatus(RegistrationStatus.VALIDATED);
        return toResponse(eventRegistrationRepository.save(reg));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> listByEvent(Long eventId) {
        return eventRegistrationRepository.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> listByParticipant(Long participantId) {
        return eventRegistrationRepository.findByParticipantIdOrderByCreatedAtDesc(participantId)
                .stream().map(this::toResponse).toList();
    }

    private EventRegistrationResponse toResponse(EventRegistration r) {
        return EventRegistrationResponse.builder()
                .id(r.getId())
                .eventId(r.getEvent().getId())
                .participantId(r.getParticipant().getId())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
