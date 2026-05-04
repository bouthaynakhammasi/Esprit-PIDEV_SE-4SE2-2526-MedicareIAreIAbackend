package com.aziz.demosec.service;

import com.aziz.demosec.dto.MedicalEventCreateRequest;
import com.aziz.demosec.dto.MedicalEventUpdateRequest;
import com.aziz.demosec.dto.MedicalEventResponse;
import com.aziz.demosec.Entities.MedicalEventType;
import com.aziz.demosec.Entities.EventParticipation;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.io.IOException;
import java.util.Optional;

public interface IMedicalEventService {
    MedicalEventResponse create(MedicalEventCreateRequest request, MultipartFile image) throws IOException;

    MedicalEventResponse update(Long id, MedicalEventUpdateRequest request, MultipartFile image) throws IOException;

    MedicalEventResponse getById(Long id);

    MedicalEventResponse getEventById(Long id);

    List<MedicalEventResponse> getAll();

    List<MedicalEventResponse> getUpcoming();

    List<MedicalEventResponse> getByType(MedicalEventType type);

    void delete(Long id);

    void participateInEvent(Long eventId, String email);

    void cancelParticipation(Long eventId, String email);

    boolean isParticipating(Long eventId, String email);

    Optional<EventParticipation> getParticipation(Long eventId, String email);

    void acceptParticipation(Long participationId);

    void rejectParticipation(Long participationId);

    List<com.aziz.demosec.dto.EventStatsResponse> getEventStats();
    org.springframework.data.domain.Page<MedicalEventResponse> searchEvents(String keyword, org.springframework.data.domain.Pageable pageable);
    
    com.aziz.demosec.dto.EventAnalyticsResponse getEventAnalytics(Long eventId);
    void markAttendance(Long eventId, Long userId, boolean attended);
    void submitFeedback(Long eventId, String email, Integer rating, String comment);
    byte[] generateTicket(Long participationId) throws IOException;
    List<com.aziz.demosec.dto.ParticipationResponse> getParticipantsByEvent(Long eventId);
}