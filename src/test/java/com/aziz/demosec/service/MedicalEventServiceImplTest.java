package com.aziz.demosec.service;

import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.MedicalEventCreateRequest;
import com.aziz.demosec.dto.MedicalEventResponse;
import com.aziz.demosec.dto.MedicalEventUpdateRequest;
import com.aziz.demosec.Entities.*;
import com.aziz.demosec.repository.MedicalEventRepository;
import com.aziz.demosec.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicalEventServiceImpl – Unit Tests")
class MedicalEventServiceImplTest {

    @Mock
    private MedicalEventRepository medicalEventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MedicalEventServiceImpl medicalEventService;

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE – ONLINE EVENT
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldCreateOnlineEventSuccessfully")
    void shouldCreateOnlineEventSuccessfully() throws IOException {
        // GIVEN
        MedicalEventCreateRequest request = new MedicalEventCreateRequest();
        request.setTitle("Webinar on Cardiology");
        request.setDescription("Live session");
        request.setDate(LocalDate.now().plusDays(5));
        request.setEventType(MedicalEventType.ONLINE);
        request.setCreatedById(1L);
        request.setPlatformName("Zoom");
        request.setMeetingLink("https://zoom.us/j/123");
        request.setMeetingPassword("pass123");

        User creator = User.builder().id(1L).fullName("Dr. Smith").email("smith@example.com").build();

        OnlineEvent savedEvent = OnlineEvent.builder()
                .id(10L)
                .title(request.getTitle())
                .description(request.getDescription())
                .date(request.getDate().atStartOfDay())
                .eventType(MedicalEventType.ONLINE)
                .createdBy(creator)
                .platformName("Zoom")
                .meetingLink("https://zoom.us/j/123")
                .meetingPassword("pass123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(medicalEventRepository.save(any(MedicalEvent.class))).thenReturn(savedEvent);

        // WHEN
        MedicalEventResponse response = medicalEventService.create(request, null);

        // THEN
        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Webinar on Cardiology", response.getTitle());
        assertEquals(MedicalEventType.ONLINE, response.getEventType());
        assertEquals("Zoom", response.getPlatformName());
        assertEquals("https://zoom.us/j/123", response.getMeetingLink());
        assertEquals(1L, response.getCreatedById());

        verify(userRepository).findById(1L);
        verify(medicalEventRepository).save(any(OnlineEvent.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE – PHYSICAL EVENT
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldCreatePhysicalEventSuccessfully")
    void shouldCreatePhysicalEventSuccessfully() throws IOException {
        // GIVEN
        MedicalEventCreateRequest request = new MedicalEventCreateRequest();
        request.setTitle("Annual Health Conference");
        request.setDate(LocalDate.now().plusDays(10));
        request.setEventType(MedicalEventType.PHYSICAL);
        request.setVenueName("Grand Hotel");
        request.setAddress("12 Rue de la Paix");
        request.setCity("Paris");
        request.setCountry("France");
        request.setCapacity(200);

        PhysicalEvent savedEvent = PhysicalEvent.builder()
                .id(20L)
                .title(request.getTitle())
                .date(request.getDate().atStartOfDay())
                .eventType(MedicalEventType.PHYSICAL)
                .venueName("Grand Hotel")
                .address("12 Rue de la Paix")
                .city("Paris")
                .country("France")
                .capacity(200)
                .build();

        when(medicalEventRepository.save(any(MedicalEvent.class))).thenReturn(savedEvent);

        // WHEN
        MedicalEventResponse response = medicalEventService.create(request, null);

        // THEN
        assertNotNull(response);
        assertEquals(20L, response.getId());
        assertEquals("Annual Health Conference", response.getTitle());
        assertEquals(MedicalEventType.PHYSICAL, response.getEventType());
        assertEquals("Grand Hotel", response.getVenueName());
        assertEquals("Paris", response.getCity());
        assertEquals(200, response.getCapacity());

        verify(medicalEventRepository).save(any(PhysicalEvent.class));
        verifyNoInteractions(userRepository);  // no createdById provided
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenCreatingEventWithUnknownUser")
    void shouldThrowExceptionWhenCreatingEventWithUnknownUser() {
        // GIVEN
        MedicalEventCreateRequest request = new MedicalEventCreateRequest();
        request.setTitle("Event");
        request.setDate(LocalDate.now().plusDays(3));
        request.setEventType(MedicalEventType.ONLINE);
        request.setCreatedById(99L);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(EntityNotFoundException.class, () -> medicalEventService.create(request, null));
        verify(medicalEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenEventTypeIsNull")
    void shouldThrowExceptionWhenEventTypeIsNull() {
        // GIVEN
        MedicalEventCreateRequest request = new MedicalEventCreateRequest();
        request.setTitle("Event");
        request.setDate(LocalDate.now().plusDays(3));
        request.setEventType(null);   // triggers the else branch → IllegalArgumentException

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> medicalEventService.create(request, null));
        verify(medicalEventRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldUpdateEventSuccessfully")
    void shouldUpdateEventSuccessfully() throws IOException {
        // GIVEN
        OnlineEvent existingEvent = OnlineEvent.builder()
                .id(10L)
                .title("Old Title")
                .date(LocalDateTime.now().plusDays(5))
                .eventType(MedicalEventType.ONLINE)
                .platformName("Teams")
                .meetingLink("https://teams.com/old")
                .build();

        MedicalEventUpdateRequest updateRequest = new MedicalEventUpdateRequest();
        updateRequest.setTitle("New Title");
        updateRequest.setDate(LocalDate.now().plusDays(10));
        updateRequest.setPlatformName("Zoom");
        updateRequest.setMeetingLink("https://zoom.us/j/999");

        when(medicalEventRepository.findById(10L)).thenReturn(Optional.of(existingEvent));
        when(medicalEventRepository.save(any(MedicalEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        MedicalEventResponse response = medicalEventService.update(10L, updateRequest, null);

        // THEN
        assertNotNull(response);
        assertEquals("New Title", response.getTitle());
        assertEquals("Zoom", response.getPlatformName());
        assertEquals("https://zoom.us/j/999", response.getMeetingLink());

        verify(medicalEventRepository).findById(10L);
        verify(medicalEventRepository).save(existingEvent);
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenUpdatingNonExistentEvent")
    void shouldThrowExceptionWhenUpdatingNonExistentEvent() {
        // GIVEN
        when(medicalEventRepository.findById(999L)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(EntityNotFoundException.class,
                () -> medicalEventService.update(999L, new MedicalEventUpdateRequest(), null));
        verify(medicalEventRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET BY ID
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldGetEventByIdSuccessfully")
    void shouldGetEventByIdSuccessfully() {
        // GIVEN
        OnlineEvent event = OnlineEvent.builder()
                .id(5L)
                .title("Test Event")
                .date(LocalDateTime.now().plusDays(2))
                .eventType(MedicalEventType.ONLINE)
                .build();

        when(medicalEventRepository.findById(5L)).thenReturn(Optional.of(event));

        // WHEN
        MedicalEventResponse response = medicalEventService.getById(5L);

        // THEN
        assertNotNull(response);
        assertEquals(5L, response.getId());
        assertEquals("Test Event", response.getTitle());
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenEventNotFound")
    void shouldThrowExceptionWhenEventNotFound() {
        // GIVEN
        when(medicalEventRepository.findById(404L)).thenReturn(Optional.empty());

        // WHEN / THEN
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> medicalEventService.getById(404L));
        assertTrue(ex.getMessage().contains("404"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET ALL
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldReturnAllEvents")
    void shouldReturnAllEvents() {
        // GIVEN
        OnlineEvent e1 = OnlineEvent.builder().id(1L).title("E1").date(LocalDateTime.now().plusDays(1)).eventType(MedicalEventType.ONLINE).build();
        PhysicalEvent e2 = PhysicalEvent.builder().id(2L).title("E2").date(LocalDateTime.now().plusDays(2)).eventType(MedicalEventType.PHYSICAL).build();
        when(medicalEventRepository.findAll()).thenReturn(List.of(e1, e2));

        // WHEN
        List<MedicalEventResponse> responses = medicalEventService.getAll();

        // THEN
        assertEquals(2, responses.size());
        verify(medicalEventRepository).findAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("shouldDeleteEventSuccessfully")
    void shouldDeleteEventSuccessfully() {
        // GIVEN
        when(medicalEventRepository.existsById(10L)).thenReturn(true);

        // WHEN
        medicalEventService.delete(10L);

        // THEN
        verify(medicalEventRepository).existsById(10L);
        verify(medicalEventRepository).deleteById(10L);
    }

    @Test
    @DisplayName("shouldThrowExceptionWhenDeletingNonExistentEvent")
    void shouldThrowExceptionWhenDeletingNonExistentEvent() {
        // GIVEN
        when(medicalEventRepository.existsById(999L)).thenReturn(false);

        // WHEN / THEN
        assertThrows(EntityNotFoundException.class, () -> medicalEventService.delete(999L));
        verify(medicalEventRepository, never()).deleteById(any());
    }
}
