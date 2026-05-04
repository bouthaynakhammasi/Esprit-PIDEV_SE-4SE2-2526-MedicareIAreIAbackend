package com.aziz.demosec.controller;

import com.aziz.demosec.dto.SaveSeatRequest;
import com.aziz.demosec.dto.SeatingStatsResponse;
import com.aziz.demosec.dto.SeatResponse;
import com.aziz.demosec.dto.SeatZoneSummaryResponse;
import com.aziz.demosec.Entities.VenueType;
import com.aziz.demosec.service.IEventSeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * REST controller for Smart Seating Management.
 * Base path: /api/events/seats
 */
@RestController
@RequestMapping("/api/events/seats")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EventSeatController {

    private final IEventSeatService seatService;

    // ── READ: get all seats for an event ──────────────────────────────────────
    @GetMapping("/{eventId}")
    public ResponseEntity<List<SeatResponse>> getEventSeats(@PathVariable Long eventId) {
        return ResponseEntity.ok(seatService.getEventSeats(eventId));
    }

    // ── READ: zone summary (totals per zone) ──────────────────────────────────
    @GetMapping("/{eventId}/summary")
    public ResponseEntity<List<SeatZoneSummaryResponse>> getEventSeatSummary(@PathVariable Long eventId) {
        return ResponseEntity.ok(seatService.getEventSeatSummary(eventId));
    }

    // ── READ: aggregated stats (total / available / reserved / blocked) ───────
    @GetMapping("/{eventId}/stats")
    public ResponseEntity<SeatingStatsResponse> getSeatingStats(@PathVariable Long eventId) {
        return ResponseEntity.ok(seatService.getSeatingStats(eventId));
    }

    // ── READ: keyword search across zone name and seat label ──────────────────
    @GetMapping("/{eventId}/search")
    public ResponseEntity<List<SeatResponse>> searchSeats(
            @PathVariable Long eventId,
            @RequestParam(value = "q", defaultValue = "") String keyword) {
        return ResponseEntity.ok(seatService.searchSeats(eventId, keyword));
    }

    // ── WRITE: batch save (admin only) ────────────────────────────────────────
    @PostMapping("/{eventId}/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> saveSeatsBatch(
            @PathVariable Long eventId,
            @RequestBody List<SaveSeatRequest> requests) {
        seatService.saveSeatsBatch(eventId, requests);
        return ResponseEntity.ok().build();
    }

    // ── WRITE: (re)generate layout for an event (admin only) ─────────────────
    /**
     * POST /api/events/seats/{eventId}/generate?venueType=HOTEL|STADIUM|CONFERENCE
     * Deletes existing seats and regenerates the full layout.
     */
    @PostMapping("/{eventId}/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateLayout(
            @PathVariable Long eventId,
            @RequestParam VenueType venueType) {
        seatService.generateLayout(eventId, venueType);
        return ResponseEntity.ok().build();
    }

    // ── WRITE: reserve a seat (any authenticated user) ────────────────────────
    @PostMapping("/{seatId}/reserve")
    public ResponseEntity<?> reserveSeat(@PathVariable Long seatId, Principal principal) {
        seatService.reserveSeat(seatId, principal.getName());
        return ResponseEntity.ok().build();
    }

    // ── WRITE: release / reset a seat (admin only) ───────────────────────────
    @PostMapping("/{seatId}/release")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> releaseSeat(@PathVariable Long seatId) {
        seatService.releaseSeat(seatId);
        return ResponseEntity.ok().build();
    }
}
