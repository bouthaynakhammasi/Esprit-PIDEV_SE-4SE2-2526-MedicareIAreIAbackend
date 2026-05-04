package com.aziz.demosec.controller;

import com.aziz.demosec.Entities.*;
import com.aziz.demosec.dto.homecare.*;
import com.aziz.demosec.service.HomeCareAssignmentService;
import com.aziz.demosec.service.HomeCareManagementService;
import com.aziz.demosec.service.ServiceRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/homecare")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HomeCareController {

    private static final Logger log = LoggerFactory.getLogger(HomeCareController.class);

    private final HomeCareManagementService homeCareService;
    private final HomeCareAssignmentService assignmentService;
    private final ServiceRecommendationService recommendationService;

    // ── AI Symptom Recommendation ─────────────────────────────────────────
    @PostMapping("/recommend-service")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ServiceRecommendationDTO> recommendService(
            @Valid @RequestBody SymptomCheckRequestDTO dto) {
        return ResponseEntity.ok(recommendationService.recommend(dto));
    }

    // ── Catalogue (PUBLIC) ─────────────────────────────────────────────────

    @GetMapping("/services")
    public ResponseEntity<List<HomeCareService>> getAllServices() {
        return ResponseEntity.ok(homeCareService.getAllActiveServices());
    }

    /**
     * Providers pour un service, triés par rating.
     * Optionnel : filtre par note minimale via ?minRating=4
     * Retourne des ProviderProfileDTO (avec avis inclus).
     */
    @GetMapping("/services/{serviceId}/providers")
    public ResponseEntity<List<ProviderProfileDTO>> getProvidersByService(
            @PathVariable("serviceId") Long serviceId,
            @RequestParam(value = "minRating", required = false) Double minRating) {
        return ResponseEntity.ok(homeCareService.searchProviders(serviceId, minRating));
    }

    @GetMapping("/services/{serviceId}/available-providers")
    public ResponseEntity<List<ProviderProfileDTO>> getAvailableProviders(
            @PathVariable("serviceId") Long serviceId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime dateTime) {
        log.info("Searching available providers for service {} at {}", serviceId, dateTime);
        return ResponseEntity.ok(homeCareService.searchAvailableProviders(serviceId, dateTime));
    }

    /**
     * Fiche complète d'un prestataire : bio, spécialités, rating, avis.
     */
    @GetMapping("/providers/{id}/profile")
    public ResponseEntity<ProviderProfileDTO> getProviderProfile(
            @PathVariable("id") Long providerId) {
        return ResponseEntity.ok(homeCareService.getProviderProfile(providerId));
    }

    /**
     * Avis du prestataire (liste triée du plus récent au plus ancien).
     */
    @GetMapping("/providers/{id}/reviews")
    public ResponseEntity<List<ServiceReview>> getProviderReviews(
            @PathVariable("id") Long providerId) {
        return ResponseEntity.ok(homeCareService.getProviderReviews(providerId));
    }

    /**
     * Créneaux disponibles d'un prestataire sur une période.
     * Exemple : GET /providers/5/slots?from=2026-03-26&to=2026-04-05
     */
    @GetMapping("/providers/{id}/slots")
    public ResponseEntity<List<AvailableSlotDTO>> getProviderSlots(
            @PathVariable("id") Long providerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(homeCareService.getProviderAvailableSlots(providerId, from, to));
    }

    /**
     * ✅ Jours bloqués/indisponibles d'un prestataire sur une période
     * Retourne les dates (YYYY-MM-DD) où le prestataire est marqué indisponible
     * (available=false)
     * Exemple : GET /providers/5/blocked-dates?from=2026-03-26&to=2026-04-05
     */
    @GetMapping("/providers/{id}/blocked-dates")
    public ResponseEntity<List<String>> getProviderBlockedDates(
            @PathVariable("id") Long providerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(homeCareService.getProviderBlockedDates(providerId, from, to));
    }

    // ── Patient ────────────────────────────────────────────────────────────

    @PostMapping("/requests")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Map<String, String>> createRequest(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CreateServiceRequestDTO dto) {
        homeCareService.createRequest(user.getUsername(), dto);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Request created successfully"));
    }

    @GetMapping("/requests/my")
    @PreAuthorize("hasAnyRole('PATIENT','VISITOR','ADMIN')")
    public ResponseEntity<List<ServiceRequest>> getMyRequests(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(homeCareService.getMyRequests(user.getUsername()));
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<ServiceRequest> getRequestById(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(homeCareService.getRequestById(id));
    }

    @PutMapping("/requests/{id}/cancel")
    public ResponseEntity<Void> cancelRequest(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails user) {
        homeCareService.cancelRequest(id, user.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{id}/review")
    public ResponseEntity<ServiceReview> submitReview(
            @PathVariable("id") Long requestId,
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody SubmitReviewDTO dto) {
        return ResponseEntity.ok(homeCareService.submitReview(requestId, user.getUsername(), dto));
    }

    @PutMapping("/requests/{id}/complete")
    public ResponseEntity<Map<String, String>> completeRequestAsPatient(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails user) {
        homeCareService.completeRequestAsPatient(id, user.getUsername());
        return ResponseEntity.ok(Map.of("status", "success", "message", "Request completed by patient"));
    }

    // ── Prestataire ────────────────────────────────────────────────────────

    @GetMapping("/provider/requests")
    @PreAuthorize("hasRole('HOME_CARE_PROVIDER')")
    public ResponseEntity<List<ServiceRequest>> getProviderRequests(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(homeCareService.getProviderRequests(user.getUsername()));
    }

    @PutMapping("/provider/requests/{id}/accept")
    @PreAuthorize("hasRole('HOME_CARE_PROVIDER')")
    public ResponseEntity<ServiceRequest> acceptRequest(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(homeCareService.acceptRequest(id, user.getUsername()));
    }

    @PutMapping("/provider/requests/{id}/decline")
    @PreAuthorize("hasRole('HOME_CARE_PROVIDER')")
    public ResponseEntity<ServiceRequest> declineRequest(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(homeCareService.declineRequest(id, user.getUsername()));
    }

    @PutMapping("/provider/requests/{id}/start")
    @PreAuthorize("hasRole('HOME_CARE_PROVIDER')")
    public ResponseEntity<ServiceRequest> startRequest(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(homeCareService.startRequest(id, user.getUsername()));
    }

    @PutMapping("/provider/requests/{id}/complete")
    @PreAuthorize("hasRole('HOME_CARE_PROVIDER')")
    public ResponseEntity<Map<String, String>> completeRequest(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody(required = false) CompleteRequestDTO dto) {
        String notes = dto != null ? dto.getProviderNotes() : null;
        homeCareService.completeRequest(id, user.getUsername(), notes);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Request completed"));
    }

    @GetMapping("/provider/availability")
    @PreAuthorize("hasRole('HOME_CARE_PROVIDER')")
    public ResponseEntity<List<ProviderAvailability>> getMyAvailability(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(homeCareService.getMyAvailability(user.getUsername()));
    }

    @PostMapping("/provider/availability")
    @PreAuthorize("hasRole('HOME_CARE_PROVIDER')")
    public ResponseEntity<ProviderAvailability> saveAvailability(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody AvailabilityDTO dto) {
        return ResponseEntity.ok(homeCareService.saveAvailability(user.getUsername(), dto));
    }

    @DeleteMapping("/provider/availability/{id}")
    @PreAuthorize("hasRole('HOME_CARE_PROVIDER')")
    public ResponseEntity<Void> deleteAvailability(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails user) {
        homeCareService.deleteAvailability(id, user.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * Bloquer un jour entier — le jour sera affiché comme indisponible pour les
     * patients.
     * Body : { "date": "2026-04-01" }
     */
    @PostMapping("/provider/calendar/block")
    @PreAuthorize("hasRole('HOME_CARE_PROVIDER')")
    public ResponseEntity<ProviderAvailability> blockDay(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody Map<String, String> body) {
        LocalDate date = LocalDate.parse(body.get("date"));
        return ResponseEntity.ok(homeCareService.blockDay(user.getUsername(), date));
    }

    /**
     * Débloquer un jour en supprimant l'entrée de blocage.
     */
    @DeleteMapping("/provider/calendar/block/{id}")
    @PreAuthorize("hasRole('HOME_CARE_PROVIDER')")
    public ResponseEntity<Void> unblockDay(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails user) {
        homeCareService.deleteAvailability(id, user.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * Vue calendrier complète du provider (règles + exceptions ponctuelles).
     */
    @GetMapping("/provider/calendar")
    @PreAuthorize("hasRole('HOME_CARE_PROVIDER')")
    public ResponseEntity<List<ProviderAvailability>> getMyCalendar(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(homeCareService.getMyAvailability(user.getUsername()));
    }

    @GetMapping("/provider/calendar/events")
    @PreAuthorize("hasRole('HOME_CARE_PROVIDER')")
    public ResponseEntity<List<CalendarEventDTO>> getCalendarEvents(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Received request for calendar events from: {}, to: {}, by user: {}", from, to, user.getUsername());
        try {
            return ResponseEntity.ok(homeCareService.getProviderCalendarEvents(user.getUsername(), from, to));
        } catch (Exception e) {
            log.error("Failed to fetch calendar events: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ── Admin ──────────────────────────────────────────────────────────────

    @GetMapping("/admin/providers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceProvider>> getAllProviders() {
        return ResponseEntity.ok(homeCareService.getAllProviders());
    }

    @GetMapping("/admin/providers/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceProvider>> getPendingProviders() {
        return ResponseEntity.ok(homeCareService.getPendingProviders());
    }

    @PutMapping("/admin/providers/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceProvider> verifyProvider(@PathVariable("id") Long id) {
        return ResponseEntity.ok(homeCareService.verifyProvider(id));
    }

    @DeleteMapping("/admin/providers/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rejectProvider(@PathVariable("id") Long id) {
        homeCareService.rejectProvider(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ServiceRequest>> getAllRequests() {
        return ResponseEntity.ok(homeCareService.getAllRequests());
    }

    @PostMapping("/admin/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HomeCareService> createService(
            @RequestBody HomeCareService service) {
        return ResponseEntity.ok(homeCareService.createService(service));
    }

    @PutMapping("/admin/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HomeCareService> updateService(
            @PathVariable("id") Long id,
            @RequestBody HomeCareService service) {
        return ResponseEntity.ok(homeCareService.updateService(id, service));
    }

    // ── Fast Booking (PATIENT) ─────────────────────────────────────────────

    @PostMapping("/requests/fast-book")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<FastBookResultDTO> fastBook(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody FastBookRequestDTO dto) {

        List<ProviderScoreDTO> ranked = assignmentService.rankProviders(dto.getServiceId(), dto.getRequestedDateTime());
        ProviderScoreDTO best = ranked.stream()
                .filter(ProviderScoreDTO::isAvailable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun prestataire disponible pour ce créneau."));

        CreateServiceRequestDTO createDto = new CreateServiceRequestDTO();
        createDto.setServiceId(dto.getServiceId());
        createDto.setRequestedDateTime(dto.getRequestedDateTime());
        createDto.setAddress(dto.getAddress());
        createDto.setPatientNotes(dto.getPatientNotes() != null ? dto.getPatientNotes() : "Fast Booking");
        createDto.setProviderId(best.getProviderId());

        ServiceRequest req = homeCareService.createRequest(user.getUsername(), createDto);

        return ResponseEntity.ok(FastBookResultDTO.builder()
                .requestId(req.getId())
                .providerName(best.getProviderName())
                .providerRating(best.getRating())
                .serviceName(req.getService().getName())
                .requestedDateTime(dto.getRequestedDateTime())
                .build());
    }

    // ── Assignment Service (ADMIN) ─────────────────────────────────────────

    @PostMapping("/admin/requests/{id}/auto-assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssignmentResultDTO> autoAssign(@PathVariable("id") Long requestId) {
        return ResponseEntity.ok(assignmentService.autoAssign(requestId));
    }

    @GetMapping("/admin/services/{serviceId}/rank-providers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProviderScoreDTO>> rankProviders(
            @PathVariable("serviceId") Long serviceId,
            @RequestParam("dateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime) {
        return ResponseEntity.ok(assignmentService.rankProviders(serviceId, dateTime));
    }
}
