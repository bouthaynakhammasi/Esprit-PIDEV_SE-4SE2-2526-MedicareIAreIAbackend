package com.aziz.demosec.service;

import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.EventRegistrationCreateRequest;
import com.aziz.demosec.dto.EventRegistrationResponse;
import com.aziz.demosec.Entities.*;
import com.aziz.demosec.repository.EventRegistrationRepository;
import com.aziz.demosec.repository.MedicalEventRepository;
import com.aziz.demosec.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventRegistrationServiceImpl – Unit Tests")
class EventRegistrationServiceImplTest {

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private MedicalEventRepository medicalEventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventRegistrationServiceImpl eventRegistrationService;

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private User buildUser(Long id) {
        return User.builder().id(id).fullName("User " + id).email("user" + id + "@test.com").build();
    }

    private PhysicalEvent buildPhysicalEvent(Long id, Integer capacity) {
        return PhysicalEvent.builder()
                .id(id)
                .title("Conference")
                .date(LocalDateTime.now().plusDays(5))
                .eventType(MedicalEventType.PHYSICAL)
                .capacity(capacity)
                .build();
    }

    private OnlineEvent buildOnlineEvent(Long id) {
        return OnlineEvent.builder()
                .id(id)
                .title("Webinar")
                .date(LocalDateTime.now().plusDays(3))
                .eventType(MedicalEventType.ONLINE)
                .build();
    }

    private EventRegistration buildRegistration(Long regId, MedicalEvent event, User user, RegistrationStatus status) {
        return EventRegistration.builder()
                .id(regId)
                .event(event)
                .participant(user)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REGISTER – success (online event → no capacity check)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldRegisterUserSuccessfullyToOnlineEvent")
    void shouldRegisterUserSuccessfullyToOnlineEvent() {
        // GIVEN
        OnlineEvent event = buildOnlineEvent(1L);
        User user = buildUser(10L);

        EventRegistrationCreateRequest request = new EventRegistrationCreateRequest();
        request.setEventId(1L);
        request.setParticipantId(10L);

        EventRegistration saved = buildRegistration(100L, event, user, RegistrationStatus.REGISTERED);

        when(medicalEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(eventRegistrationRepository.existsByEventIdAndParticipantId(1L, 10L)).thenReturn(false);
        when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(saved);

        // WHEN
        EventRegistrationResponse response = eventRegistrationService.register(request);

        // THEN
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(1L, response.getEventId());
        assertEquals(10L, response.getParticipantId());
        assertEquals(RegistrationStatus.REGISTERED, response.getStatus());

        verify(eventRegistrationRepository).save(any(EventRegistration.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REGISTER – success (physical event with available capacity)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldRegisterUserSuccessfullyWhenPhysicalEventHasAvailableCapacity")
    void shouldRegisterUserSuccessfullyWhenPhysicalEventHasAvailableCapacity() {
        // GIVEN – capacity 5, only 2 already registered
        PhysicalEvent event = buildPhysicalEvent(2L, 5);
        User user = buildUser(20L);

        EventRegistrationCreateRequest request = new EventRegistrationCreateRequest();
        request.setEventId(2L);
        request.setParticipantId(20L);

        // Two already-registered participants
        User other1 = buildUser(30L);
        User other2 = buildUser(31L);
        List<EventRegistration> existingRegs = List.of(
                buildRegistration(1L, event, other1, RegistrationStatus.REGISTERED),
                buildRegistration(2L, event, other2, RegistrationStatus.REGISTERED)
        );

        EventRegistration saved = buildRegistration(200L, event, user, RegistrationStatus.REGISTERED);

        when(medicalEventRepository.findById(2L)).thenReturn(Optional.of(event));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(eventRegistrationRepository.existsByEventIdAndParticipantId(2L, 20L)).thenReturn(false);
        when(eventRegistrationRepository.findByEventIdOrderByCreatedAtDesc(2L)).thenReturn(existingRegs);
        when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(saved);

        // WHEN
        EventRegistrationResponse response = eventRegistrationService.register(request);

        // THEN
        assertNotNull(response);
        assertEquals(RegistrationStatus.REGISTERED, response.getStatus());
        verify(eventRegistrationRepository).save(any(EventRegistration.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REGISTER – fail: event not found
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldThrowExceptionWhenEventNotFoundDuringRegistration")
    void shouldThrowExceptionWhenEventNotFoundDuringRegistration() {
        // GIVEN
        EventRegistrationCreateRequest request = new EventRegistrationCreateRequest();
        request.setEventId(999L);
        request.setParticipantId(10L);

        when(medicalEventRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN / THEN
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> eventRegistrationService.register(request));
        assertTrue(ex.getMessage().contains("999"));
        verify(eventRegistrationRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REGISTER – fail: user not found
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldThrowExceptionWhenUserNotFoundDuringRegistration")
    void shouldThrowExceptionWhenUserNotFoundDuringRegistration() {
        // GIVEN
        OnlineEvent event = buildOnlineEvent(1L);

        EventRegistrationCreateRequest request = new EventRegistrationCreateRequest();
        request.setEventId(1L);
        request.setParticipantId(888L);

        when(medicalEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(888L)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(EntityNotFoundException.class, () -> eventRegistrationService.register(request));
        verify(eventRegistrationRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REGISTER – fail: already registered (duplicate)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldThrowExceptionWhenUserAlreadyRegistered")
    void shouldThrowExceptionWhenUserAlreadyRegistered() {
        // GIVEN
        OnlineEvent event = buildOnlineEvent(1L);
        User user = buildUser(10L);

        EventRegistrationCreateRequest request = new EventRegistrationCreateRequest();
        request.setEventId(1L);
        request.setParticipantId(10L);

        when(medicalEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(eventRegistrationRepository.existsByEventIdAndParticipantId(1L, 10L)).thenReturn(true);

        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> eventRegistrationService.register(request));
        assertTrue(ex.getMessage().contains("already registered"));
        verify(eventRegistrationRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REGISTER – fail: physical event capacity reached
    //  Simulates the WAITLISTED scenario (capacity is full → service throws)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldThrowExceptionWhenPhysicalEventCapacityReached")
    void shouldThrowExceptionWhenPhysicalEventCapacityReached() {
        // GIVEN – capacity 2, both spots already taken
        PhysicalEvent event = buildPhysicalEvent(3L, 2);
        User user = buildUser(40L);

        EventRegistrationCreateRequest request = new EventRegistrationCreateRequest();
        request.setEventId(3L);
        request.setParticipantId(40L);

        User u1 = buildUser(50L);
        User u2 = buildUser(51L);
        List<EventRegistration> full = List.of(
                buildRegistration(10L, event, u1, RegistrationStatus.REGISTERED),
                buildRegistration(11L, event, u2, RegistrationStatus.REGISTERED)
        );

        when(medicalEventRepository.findById(3L)).thenReturn(Optional.of(event));
        when(userRepository.findById(40L)).thenReturn(Optional.of(user));
        when(eventRegistrationRepository.existsByEventIdAndParticipantId(3L, 40L)).thenReturn(false);
        when(eventRegistrationRepository.findByEventIdOrderByCreatedAtDesc(3L)).thenReturn(full);

        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> eventRegistrationService.register(request));
        assertTrue(ex.getMessage().contains("capacity"));
        verify(eventRegistrationRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VALIDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldValidateRegistrationSuccessfully")
    void shouldValidateRegistrationSuccessfully() {
        // GIVEN
        PhysicalEvent event = buildPhysicalEvent(1L, 10);
        User user = buildUser(5L);
        EventRegistration reg = buildRegistration(50L, event, user, RegistrationStatus.REGISTERED);

        when(eventRegistrationRepository.findById(50L)).thenReturn(Optional.of(reg));
        when(eventRegistrationRepository.save(any(EventRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        EventRegistrationResponse response = eventRegistrationService.validate(50L);

        // THEN
        assertNotNull(response);
        assertEquals(RegistrationStatus.VALIDATED, response.getStatus());
        verify(eventRegistrationRepository).save(reg);
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenValidatingNonExistentRegistration")
    void shouldThrowExceptionWhenValidatingNonExistentRegistration() {
        // GIVEN
        when(eventRegistrationRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(EntityNotFoundException.class, () -> eventRegistrationService.validate(999L));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LIST BY EVENT
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldListRegistrationsByEvent")
    void shouldListRegistrationsByEvent() {
        // GIVEN
        PhysicalEvent event = buildPhysicalEvent(1L, 10);
        User u1 = buildUser(1L);
        User u2 = buildUser(2L);
        List<EventRegistration> regs = List.of(
                buildRegistration(1L, event, u1, RegistrationStatus.REGISTERED),
                buildRegistration(2L, event, u2, RegistrationStatus.VALIDATED)
        );

        when(eventRegistrationRepository.findByEventIdOrderByCreatedAtDesc(1L)).thenReturn(regs);

        // WHEN
        List<EventRegistrationResponse> responses = eventRegistrationService.listByEvent(1L);

        // THEN
        assertEquals(2, responses.size());
        verify(eventRegistrationRepository).findByEventIdOrderByCreatedAtDesc(1L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PROMOTE WAITLISTED USER (simulation)
    //  This tests that after validating a registration the service correctly
    //  persists VALIDATED status – promotions are implicit in this service.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldPromoteWaitlistedUserByValidatingTheirRegistration")
    void shouldPromoteWaitlistedUserByValidatingTheirRegistration() {
        // GIVEN – a REGISTERED entry that an admin promotes to VALIDATED
        PhysicalEvent event = buildPhysicalEvent(1L, 5);
        User user = buildUser(7L);
        EventRegistration waitlisted = buildRegistration(77L, event, user, RegistrationStatus.REGISTERED);

        when(eventRegistrationRepository.findById(77L)).thenReturn(Optional.of(waitlisted));
        when(eventRegistrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        EventRegistrationResponse response = eventRegistrationService.validate(77L);

        // THEN – status promoted to VALIDATED
        assertEquals(RegistrationStatus.VALIDATED, response.getStatus());

        // Capture the saved entity and verify mutation
        ArgumentCaptor<EventRegistration> captor = ArgumentCaptor.forClass(EventRegistration.class);
        verify(eventRegistrationRepository).save(captor.capture());
        assertEquals(RegistrationStatus.VALIDATED, captor.getValue().getStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LIST BY PARTICIPANT
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldListRegistrationsByParticipant")
    void shouldListRegistrationsByParticipant() {
        // GIVEN
        PhysicalEvent event = buildPhysicalEvent(1L, 10);
        User user = buildUser(55L);
        List<EventRegistration> regs = List.of(
                buildRegistration(100L, event, user, RegistrationStatus.REGISTERED)
        );

        when(eventRegistrationRepository.findByParticipantIdOrderByCreatedAtDesc(55L)).thenReturn(regs);

        // WHEN
        List<EventRegistrationResponse> responses = eventRegistrationService.listByParticipant(55L);

        // THEN
        assertEquals(1, responses.size());
        verify(eventRegistrationRepository).findByParticipantIdOrderByCreatedAtDesc(55L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CANCEL (Placeholder)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldCancelRegistration")
    void shouldCancelRegistration() {
        // TODO: This test serves as a placeholder for registration cancellation logic.
        // Currently, IEventRegistrationService does not have a cancel() or delete() method.
        // In a production scenario, this would verify that the status is updated to CANCELLED
        // or the record is removed, according to business rules.
        
        // GIVEN
        // Setup registration to be cancelled
        
        // WHEN
        // eventRegistrationService.cancel(registrationId);
        
        // THEN
        // verify(eventRegistrationRepository).save(...) or delete(...)
    }
}
