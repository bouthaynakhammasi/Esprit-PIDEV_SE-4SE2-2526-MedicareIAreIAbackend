package com.aziz.demosec.service;

import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.*;
import com.aziz.demosec.Entities.*;
import com.aziz.demosec.repository.MedicalEventRepository;
import com.aziz.demosec.repository.UserRepository;
import com.aziz.demosec.repository.EventParticipationRepository;
import com.aziz.demosec.repository.NotificationRepository;
import com.aziz.demosec.repository.EventFeedbackRepository;
import com.aziz.demosec.domain.Role;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.itextpdf.html2pdf.HtmlConverter;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalEventServiceImpl implements IMedicalEventService {

    private final MedicalEventRepository medicalEventRepository;
    private final UserRepository userRepository;
    private final EventParticipationRepository participationRepository;
    private final NotificationRepository notificationRepository;
    private final INotificationService notificationService;
    private final com.aziz.demosec.repository.EventFeedbackRepository feedbackRepository;
    /** Injected for smart seating auto-generation after event creation. */
    private final IEventSeatService seatService;
    private final com.aziz.demosec.repository.EventSeatRepository seatRepository;

    @Override
    public MedicalEventResponse create(MedicalEventCreateRequest request, MultipartFile image) throws IOException {
        User creator = null;
        if (request.getCreatedById() != null) {
            creator = userRepository.findById(request.getCreatedById())
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getCreatedById()));
        }

        String imageUrl = request.getImageUrl();
        if (image != null && !image.isEmpty()) {
            imageUrl = "data:" + image.getContentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(image.getBytes());
        }

        MedicalEvent event;
        LocalDateTime eventDate = request.getDate();

        if (request.getEventType() == MedicalEventType.ONLINE) {
            event = OnlineEvent.builder()
                    .title(request.getTitle()).description(request.getDescription())
                    .date(eventDate).eventType(MedicalEventType.ONLINE)
                    .createdBy(creator).imageUrl(imageUrl)
                    .platformName(request.getPlatformName()).meetingLink(request.getMeetingLink())
                    .meetingPassword(request.getMeetingPassword())
                    .capacity(request.getCapacity() != null ? request.getCapacity() : 0)
                    .ticketPrice(request.getTicketPrice() != null ? request.getTicketPrice() : 0.0)
                    .build();
        } else {
            event = PhysicalEvent.builder()
                    .title(request.getTitle()).description(request.getDescription())
                    .date(eventDate).eventType(MedicalEventType.PHYSICAL)
                    .createdBy(creator).imageUrl(imageUrl)
                    .venueName(request.getVenueName()).address(request.getAddress())
                    .city(request.getCity()).postalCode(request.getPostalCode())
                    .country(request.getCountry())
                    .venueType(request.getVenueType())
                    .capacity(request.getCapacity() != null ? request.getCapacity() : 0)
                    .ticketPrice(request.getTicketPrice() != null ? request.getTicketPrice() : 0.0)
                    .build();
        }

        MedicalEvent saved = medicalEventRepository.save(event);

        // ── Auto-generate seating layout for physical events ──────────────────
        if (saved instanceof PhysicalEvent && request.getVenueType() != null) {
            seatService.generateLayout(saved.getId(), request.getVenueType());
        }

        return toResponse(saved);
    }

    @Override
    public MedicalEventResponse update(Long id, MedicalEventUpdateRequest request, MultipartFile image) throws IOException {
        MedicalEvent e = medicalEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MedicalEvent not found: " + id));

        if (request.getTitle() != null) e.setTitle(request.getTitle());
        if (request.getDescription() != null) e.setDescription(request.getDescription());
        if (request.getDate() != null) e.setDate(request.getDate());
        if (request.getCapacity() != null) e.setCapacity(request.getCapacity());
        if (request.getTicketPrice() != null) e.setTicketPrice(request.getTicketPrice());

        if (image != null && !image.isEmpty()) {
            String b64 = "data:" + image.getContentType() + ";base64," + Base64.getEncoder().encodeToString(image.getBytes());
            e.setImageUrl(b64);
        } else if (request.getImageUrl() != null) {
            e.setImageUrl(request.getImageUrl());
        }

        if (e instanceof OnlineEvent oe) {
            if (request.getPlatformName() != null) oe.setPlatformName(request.getPlatformName());
            if (request.getMeetingLink() != null) oe.setMeetingLink(request.getMeetingLink());
            if (request.getMeetingPassword() != null) oe.setMeetingPassword(request.getMeetingPassword());
        } else if (e instanceof PhysicalEvent pe) {
            if (request.getVenueName() != null) pe.setVenueName(request.getVenueName());
            if (request.getAddress() != null) pe.setAddress(request.getAddress());
            if (request.getCity() != null) pe.setCity(request.getCity());
            if (request.getPostalCode() != null) pe.setPostalCode(request.getPostalCode());
            if (request.getCountry() != null) pe.setCountry(request.getCountry());
            if (request.getVenueType() != null) pe.setVenueType(request.getVenueType());
        }
        return toResponse(medicalEventRepository.save(e));
    }

    private MedicalEventResponse toResponse(MedicalEvent e) {
        MedicalEventResponse.MedicalEventResponseBuilder b = MedicalEventResponse.builder()
                .id(e.getId()).title(e.getTitle()).description(e.getDescription())
                .date(e.getDate()).eventType(e.getEventType())
                .createdById(e.getCreatedBy() != null ? e.getCreatedBy().getId() : null)
                .capacity(e.getCapacity()).imageUrl(e.getImageUrl())
                .ticketPrice(e.getTicketPrice());
        if (e instanceof OnlineEvent oe) {
            b.platformName(oe.getPlatformName()).meetingLink(oe.getMeetingLink()).meetingPassword(oe.getMeetingPassword());
        }
        if (e instanceof PhysicalEvent pe) {
            b.venueName(pe.getVenueName()).address(pe.getAddress())
                    .city(pe.getCity()).postalCode(pe.getPostalCode()).country(pe.getCountry())
                    .venueType(pe.getVenueType() != null ? pe.getVenueType().name() : null);
        }
        return b.build();
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalEventResponse getById(Long id) {
        return toResponse(medicalEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MedicalEvent not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalEventResponse> getAll() {
        return medicalEventRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalEventResponse> getUpcoming() {
        return medicalEventRepository.findByDateAfterOrderByDateAsc(LocalDateTime.now())
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalEventResponse> getByType(MedicalEventType type) {
        return medicalEventRepository.findByEventType(type)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public void delete(Long id) {
        if (!medicalEventRepository.existsById(id)) {
            throw new EntityNotFoundException("MedicalEvent not found: " + id);
        }
        seatRepository.deleteAllByEventId(id);
        medicalEventRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalEventResponse getEventById(Long id) {
        return getById(id);
    }

    @Override
    public void participateInEvent(Long eventId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + email));
        MedicalEvent event = medicalEventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("MedicalEvent not found: " + eventId));

        boolean alreadyJoined = participationRepository
                .existsByEventIdAndPatientId(event.getId(), user.getId());

        if (alreadyJoined) {
            throw new RuntimeException("Already registered for this event");
        }

        EventParticipation p = EventParticipation.builder()
                .event(event)
                .patient(user)
                .registeredAt(LocalDateTime.now())
                .status("PENDING")
                .build();

        EventParticipation savedParticipation = participationRepository.save(p);

        userRepository.findByRole(Role.ADMIN).forEach(admin -> {
            Notification notification = Notification.builder()
                    .recipient(admin).sender(user)
                    .title("New Event Participation Request")
                    .message(user.getFullName() + " wants to join: \"" + event.getTitle() + "\"")
                    .type(NotificationType.EVENT_JOIN)
                    .targetId(eventId).participationId(savedParticipation.getId())
                    .isRead(false).createdAt(LocalDateTime.now()).build();
            notificationService.sendNotification(notification);
        });
    }

    @Override
    public void acceptParticipation(Long participationId) {
        EventParticipation p = participationRepository.findById(participationId).orElseThrow();
        p.setStatus("CONFIRMED");
        participationRepository.save(p);

        User admin = userRepository.findByRole(Role.ADMIN).stream().findFirst().orElse(null);

        Notification notif = Notification.builder()
                .recipient(p.getPatient()).sender(admin)
                .title("Participation Accepted")
                .message("✅ Your participation in \"" + p.getEvent().getTitle() + "\" has been ACCEPTED!")
                .type(NotificationType.PARTICIPATION_ACCEPTED)
                .targetId(p.getEvent().getId())
                .participationId(participationId).isRead(false).createdAt(LocalDateTime.now()).build();        notificationService.sendNotification(notif);
    }

    @Override
    public void rejectParticipation(Long participationId) {
        EventParticipation p = participationRepository.findById(participationId).orElseThrow();
        p.setStatus("REJECTED");
        participationRepository.save(p);

        Notification notif = Notification.builder()
                .recipient(p.getPatient())
                .title("Participation Rejected")
                .message("❌ Your participation in \"" + p.getEvent().getTitle() + "\" has been rejected.")
                .type(NotificationType.PARTICIPATION_REJECTED)
                .targetId(p.getEvent().getId())
                .participationId(participationId).isRead(false).createdAt(LocalDateTime.now()).build();
        notificationService.sendNotification(notif);
    }
    @Override
    public void cancelParticipation(Long eventId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        participationRepository.deleteByEventIdAndPatientId(eventId, user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isParticipating(Long eventId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return participationRepository.existsByEventIdAndPatientId(eventId, user.getId());
    }

    public Optional<EventParticipation> getParticipation(Long eventId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return participationRepository.findByEventIdAndPatientId(eventId, user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventStatsResponse> getEventStats() {
        return medicalEventRepository.getEventStats();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MedicalEventResponse> searchEvents(String keyword, Pageable pageable) {
        return medicalEventRepository.searchEvents(keyword, pageable).map(this::toResponse);
    }

    @Override
    public com.aziz.demosec.dto.EventAnalyticsResponse getEventAnalytics(Long eventId) {
        MedicalEvent event = medicalEventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        long registrations = participationRepository.countByEventId(eventId);
        long attendance = participationRepository.countByEventIdAndAttendedTrue(eventId);
        Double avgRating = feedbackRepository.getAverageRating(eventId);
        double revenue = attendance * (event.getTicketPrice() != null ? event.getTicketPrice() : 0.0);

        // Comparison
        Double attendanceDrift = 0.0;
        Double satisfactionDrift = 0.0;
        List<MedicalEvent> previous = medicalEventRepository.findPreviousEvents(event.getDate(), org.springframework.data.domain.PageRequest.of(0, 1));
        if (!previous.isEmpty()) {
            MedicalEvent prev = previous.get(0);
            long prevRegs = participationRepository.countByEventId(prev.getId());
            long prevAtt = participationRepository.countByEventIdAndAttendedTrue(prev.getId());
            double prevRate = prevRegs > 0 ? (double) prevAtt / prevRegs : 0;
            double currentRate = registrations > 0 ? (double) attendance / registrations : 0;
            attendanceDrift = (currentRate - prevRate) * 100;

            Double prevSat = feedbackRepository.getAverageRating(prev.getId());
            if (prevSat != null && avgRating != null) {
                satisfactionDrift = avgRating - prevSat;
            }
        }

        List<String> recs = new java.util.ArrayList<>();
        if (registrations > 0 && (double)attendance/registrations < 0.5) recs.add("Low attendance: Consider sending more reminders.");
        if (avgRating != null && avgRating < 3.5) recs.add("Quality issues: Review feedback comments for content improvement.");
        if (revenue < 100) recs.add("Low revenue: Review pricing strategy or sponsorship.");

        return com.aziz.demosec.dto.EventAnalyticsResponse.builder()
                .eventId(eventId).eventTitle(event.getTitle())
                .totalRegistrations(registrations).actualAttendance(attendance)
                .attendanceRate(registrations > 0 ? (double) attendance / registrations * 100 : 0)
                .averageSatisfaction(avgRating != null ? avgRating : 0.0)
                .totalRevenue(revenue)
                .attendanceDrift(attendanceDrift).satisfactionDrift(satisfactionDrift)
                .recommendations(recs)
                .build();
    }

    @Override
    public void markAttendance(Long eventId, Long userId, boolean attended) {
        EventParticipation p = participationRepository.findByEventIdAndPatientId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("Participation not found"));
        p.setAttended(attended);
        participationRepository.save(p);
    }

    @Override
    public void submitFeedback(Long eventId, String email, Integer rating, String comment) {
        User user = userRepository.findByEmail(email).orElseThrow();
        MedicalEvent event = medicalEventRepository.findById(eventId).orElseThrow();
        EventFeedback feedback = feedbackRepository.findByEventIdAndUserId(eventId, user.getId())
                .orElse(new EventFeedback());
        feedback.setEvent(event);
        feedback.setUser(user);
        feedback.setRating(rating);
        feedback.setComment(comment);
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackRepository.save(feedback);
    }

    @Override
    public byte[] generateTicket(Long participationId) throws IOException {
        EventParticipation p = participationRepository.findById(participationId)
                .orElseThrow(() -> new EntityNotFoundException("Participation not found"));

        if (!"CONFIRMED".equals(p.getStatus())) {
            throw new RuntimeException("Ticket only available for confirmed participations");
        }

        String html = """
            <html>
            <body style="font-family: Arial, sans-serif; padding: 20px; border: 2px solid #2563eb; border-radius: 10px;">
                <h1 style="color: #2563eb; text-align: center;">MEDICAREAI - EVENT TICKET</h1>
                <hr>
                <div style="margin-top: 20px;">
                    <p><strong>Event:</strong> %s</p>
                    <p><strong>Date:</strong> %s</p>
                    <p><strong>Participant:</strong> %s</p>
                    <p><strong>Status:</strong> %s</p>
                </div>
                <br><br>
                <div style="background-color: #f3f4f6; padding: 20px; border-radius: 10px; text-align: center;">
                    <p style="font-size: 0.9rem; color: #6b7280; margin-bottom: 10px;">Unique Ticket Number</p>
                    <h2 style="letter-spacing: 10px; margin: 0; color: #1f2937;">TKT-%d</h2>
                </div>
                <div style="margin-top: 30px; text-align: center; font-size: 0.8rem; color: #9ca3af;">
                    Thank you for using MEDICAREAI
                </div>
            </body>
            </html>
            """.formatted(
                p.getEvent().getTitle(),
                p.getEvent().getDate().toString(),
                p.getPatient().getFullName(),
                p.getStatus(),
                p.getId()
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, baos);
        return baos.toByteArray();
    }

    @Override
    public List<ParticipationResponse> getParticipantsByEvent(Long eventId) {
        return participationRepository.findByEventId(eventId).stream()
                .map(p -> ParticipationResponse.builder()
                        .id(p.getId())
                        .eventId(p.getEvent().getId())
                        .userId(p.getPatient().getId())
                        .userFullName(p.getPatient().getFullName())
                        .userEmail(p.getPatient().getEmail())
                        .status(p.getStatus())
                        .registeredAt(p.getRegisteredAt())
                        .attended(p.isAttended())
                        .build())
                .collect(Collectors.toList());
    }
}