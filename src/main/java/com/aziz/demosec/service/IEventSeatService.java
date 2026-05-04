package com.aziz.demosec.service;

import com.aziz.demosec.dto.SaveSeatRequest;
import com.aziz.demosec.dto.SeatingStatsResponse;
import com.aziz.demosec.dto.SeatResponse;
import com.aziz.demosec.dto.SeatZoneSummaryResponse;
import com.aziz.demosec.Entities.VenueType;

import java.util.List;

public interface IEventSeatService {

    // ── Existing ─────────────────────────────────────────────────────────────
    List<SeatResponse>         getEventSeats(Long eventId);
    List<SeatZoneSummaryResponse> getEventSeatSummary(Long eventId);
    void saveSeatsBatch(Long eventId, List<SaveSeatRequest> requests);
    void reserveSeat(Long seatId, String userEmail);
    void releaseSeat(Long seatId);

    // ── Smart Seating ─────────────────────────────────────────────────────────
    /** Auto-generate a full seating layout based on venue type. Clears existing seats first. */
    void generateLayout(Long eventId, VenueType venueType);

    /** Aggregated stats: total / available / reserved / blocked + per-zone breakdown. */
    SeatingStatsResponse getSeatingStats(Long eventId);

    /** Keyword search across zone name and seat label. */
    List<SeatResponse> searchSeats(Long eventId, String keyword);
}
