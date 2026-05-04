package com.aziz.demosec.controller;

import com.aziz.demosec.dto.MedicalEventCreateRequest;
import com.aziz.demosec.dto.MedicalEventUpdateRequest;
import com.aziz.demosec.dto.MedicalEventResponse;
import com.aziz.demosec.dto.EventStatsResponse;
import com.aziz.demosec.service.IMedicalEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class MedicalEventController {
    private final IMedicalEventService medicalEventService;

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicalEventResponse> create(
            @RequestPart("event") MedicalEventCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
        return ResponseEntity.ok(medicalEventService.create(request, image));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MedicalEventResponse> update(
            @PathVariable Long id,
            @RequestPart("event") MedicalEventUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
        return ResponseEntity.ok(medicalEventService.update(id, request, image));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<MedicalEventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(medicalEventService.getEventById(id));
    }

    @GetMapping
    public ResponseEntity<List<MedicalEventResponse>> getAll() {
        return ResponseEntity.ok(medicalEventService.getAll());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<MedicalEventResponse>> getUpcoming() {
        return ResponseEntity.ok(medicalEventService.getUpcoming());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        medicalEventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/participate")
    @PreAuthorize("!hasRole('ADMIN')")
    public ResponseEntity<Void> participate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        medicalEventService.participateInEvent(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/cancel-participation")
    @PreAuthorize("!hasRole('ADMIN')")
    public ResponseEntity<Void> cancelParticipation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        medicalEventService.cancelParticipation(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/is-participating")
    public ResponseEntity<?> isParticipating(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Optional<com.aziz.demosec.Entities.EventParticipation> p = medicalEventService.getParticipation(id, userDetails.getUsername());
        if (p.isPresent()) {
            return ResponseEntity.ok(Map.of(
                "participating", true,
                "status", p.get().getStatus(),
                "participationId", p.get().getId()
            ));
        }
        return ResponseEntity.ok(Map.of("participating", false));
    }

    @PutMapping("/participation/{participationId}/accept")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> acceptParticipation(
            @PathVariable Long participationId) {
        medicalEventService.acceptParticipation(participationId);
        return ResponseEntity.ok(Map.of("message", "Participation accepted"));
    }

    @PutMapping("/participation/{participationId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectParticipation(
            @PathVariable Long participationId) {
        medicalEventService.rejectParticipation(participationId);
        return ResponseEntity.ok(Map.of("message", "Participation rejected"));
    }

    @GetMapping("/stats")
    public ResponseEntity<List<EventStatsResponse>> getEventStats() {
        return ResponseEntity.ok(medicalEventService.getEventStats());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<MedicalEventResponse>> searchEvents(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(medicalEventService.searchEvents(keyword, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.aziz.demosec.dto.EventAnalyticsResponse> getEventAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(medicalEventService.getEventAnalytics(id));
    }

    @PostMapping("/{id}/attendance/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markAttendance(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestParam boolean attended) {
        medicalEventService.markAttendance(id, userId, attended);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("!hasRole('ADMIN')")
    public ResponseEntity<Void> submitFeedback(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> payload) {
        Integer rating = (Integer) payload.get("rating");
        String comment = (String) payload.get("comment");
        medicalEventService.submitFeedback(id, userDetails.getUsername(), rating, comment);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/participation/{participationId}/ticket")
    public ResponseEntity<byte[]> downloadTicket(@PathVariable Long participationId) throws IOException {
        byte[] pdf = medicalEventService.generateTicket(participationId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=ticket-" + participationId + ".pdf")
                .body(pdf);
    }

    @GetMapping("/{id}/participants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.aziz.demosec.dto.ParticipationResponse>> getParticipants(@PathVariable Long id) {
        return ResponseEntity.ok(medicalEventService.getParticipantsByEvent(id));
    }
}