package com.aziz.demosec.service;

import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.*;
import com.aziz.demosec.Entities.*;
import com.aziz.demosec.repository.EventSeatRepository;
import com.aziz.demosec.repository.MedicalEventRepository;
import com.aziz.demosec.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSeatServiceImpl implements IEventSeatService {

    private final EventSeatRepository seatRepository;
    private final MedicalEventRepository eventRepository;
    private final UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // EXISTING METHODS
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<SeatResponse> getEventSeats(Long eventId) {
        return seatRepository.findByEventId(eventId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<SeatZoneSummaryResponse> getEventSeatSummary(Long eventId) {
        return seatRepository.getSeatSummaryByZone(eventId);
    }

    @Override
    @Transactional
    public void saveSeatsBatch(Long eventId, List<SaveSeatRequest> requests) {
        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        for (SaveSeatRequest req : requests) {
            EventSeat seat;
            if (req.getId() != null) {
                seat = seatRepository.findById(req.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Seat not found: " + req.getId()));
            } else {
                seat = new EventSeat();
                seat.setEvent(event);
            }
            seat.setZoneName(req.getZoneName());
            seat.setSeatLabel(req.getSeatLabel());
            seat.setPosX(req.getPosX());
            seat.setPosY(req.getPosY());
            if (seat.getStatus() == null || seat.getStatus() == SeatStatus.AVAILABLE
                    || seat.getStatus() == SeatStatus.BLOCKED) {
                seat.setStatus(SeatStatus.valueOf(req.getStatus()));
            }
            seatRepository.save(seat);
        }
    }

    @Override
    @Transactional
    public void reserveSeat(Long seatId, String userEmail) {
        EventSeat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new EntityNotFoundException("Seat not found: " + seatId));

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new RuntimeException("Seat " + seat.getSeatLabel() + " is not available");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userEmail));

        seat.setStatus(SeatStatus.RESERVED);
        seat.setReservedBy(user);
        seatRepository.save(seat);
    }

    @Override
    @Transactional
    public void releaseSeat(Long seatId) {
        EventSeat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new EntityNotFoundException("Seat not found: " + seatId));
        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setReservedBy(null);
        seatRepository.save(seat);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SMART SEATING — AUTO LAYOUT GENERATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Auto-generates a complete seating layout for the given event.
     * Any existing seats for the event are deleted before regeneration.
     *
     * HOTEL      → 10 tables × 8 seats   =  80 seats
     * STADIUM    → 3 sections × 10 rows × 20 seats = 600 seats
     * CONFERENCE → 15 rows × 10 seats    = 150 seats
     */
    @Override
    @Transactional
    public void generateLayout(Long eventId, VenueType venueType) {
        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        // Save venueType if this is a PhysicalEvent
        if (event instanceof PhysicalEvent pe) {
            pe.setVenueType(venueType);
            eventRepository.save(pe);
        }

        // Clear existing seats to allow regeneration
        seatRepository.deleteAllByEventId(eventId);

        List<EventSeat> seats = new ArrayList<>();

        switch (venueType) {

            case HOTEL -> {
                // 10 round tables, 8 seats per table
                for (int t = 1; t <= 10; t++) {
                    for (int s = 1; s <= 8; s++) {
                        seats.add(EventSeat.builder()
                                .event(event)
                                .zoneName("Table " + t)
                                .seatLabel("T" + t + "-S" + s)
                                .tableNumber(t)
                                .seatNumber(s)
                                .rowNumber(null)
                                .status(SeatStatus.AVAILABLE)
                                .build());
                    }
                }
            }

            case STADIUM -> {
                // Sections A, B, C — each has 10 rows × 20 seats
                String[] sections = {"A", "B", "C"};
                for (String sec : sections) {
                    for (int r = 1; r <= 10; r++) {
                        for (int s = 1; s <= 20; s++) {
                            seats.add(EventSeat.builder()
                                    .event(event)
                                    .zoneName("Section " + sec)
                                    .seatLabel(sec + "-R" + r + "-" + s)
                                    .tableNumber(null)
                                    .rowNumber(r)
                                    .seatNumber(s)
                                    .status(SeatStatus.AVAILABLE)
                                    .build());
                        }
                    }
                }
            }

            case CONFERENCE -> {
                // 15 rows × 10 seats facing a central stage
                for (int r = 1; r <= 15; r++) {
                    for (int s = 1; s <= 10; s++) {
                        seats.add(EventSeat.builder()
                                .event(event)
                                .zoneName("Main Hall")
                                .seatLabel("R" + r + "-S" + s)
                                .tableNumber(null)
                                .rowNumber(r)
                                .seatNumber(s)
                                .status(SeatStatus.AVAILABLE)
                                .build());
                    }
                }
            }

            default -> throw new IllegalArgumentException("Unsupported VenueType: " + venueType);
        }

        seatRepository.saveAll(seats);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SMART SEATING — STATS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns aggregated seating statistics for an event.
     * Uses a single JPQL aggregate query (SUM/CASE) for total counts,
     * then fetches per-zone breakdown using getSeatSummaryByZone.
     */
    @Override
    @Transactional(readOnly = true)
    public SeatingStatsResponse getSeatingStats(Long eventId) {
        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        // JPQL aggregate: [0]=total, [1]=available, [2]=reserved, [3]=blocked
        List<Object[]> rows = seatRepository.getTotalSeatingStats(eventId);
        long total = 0L, available = 0L, reserved = 0L, blocked = 0L;
        if (rows != null && !rows.isEmpty()) {
            Object[] raw = rows.get(0);
            total     = raw[0] != null ? ((Number) raw[0]).longValue() : 0L;
            available = raw[1] != null ? ((Number) raw[1]).longValue() : 0L;
            reserved  = raw[2] != null ? ((Number) raw[2]).longValue() : 0L;
            blocked   = raw[3] != null ? ((Number) raw[3]).longValue() : 0L;
        }

        List<SeatZoneSummaryResponse> byZone = seatRepository.getSeatSummaryByZone(eventId);

        // Resolve venueType from PhysicalEvent sub-class
        String venueType = null;
        if (event instanceof PhysicalEvent pe && pe.getVenueType() != null) {
            venueType = pe.getVenueType().name();
        }

        return SeatingStatsResponse.builder()
                .eventId(eventId)
                .eventTitle(event.getTitle())
                .venueType(venueType)
                .totalSeats(total)
                .availableSeats(available)
                .reservedSeats(reserved)
                .blockedSeats(blocked)
                .byZone(byZone)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SMART SEATING — KEYWORD SEARCH
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Searches seats by zone name or seat label (case-insensitive).
     * Results can be further filtered by status on the frontend.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> searchSeats(Long eventId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getEventSeats(eventId);
        }
        return seatRepository.searchSeats(eventId, keyword.trim())
                .stream().map(this::toResponse).toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPING HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private SeatResponse toResponse(EventSeat s) {
        return SeatResponse.builder()
                .id(s.getId())
                .eventId(s.getEvent().getId())
                .zoneName(s.getZoneName())
                .seatLabel(s.getSeatLabel())
                .posX(s.getPosX())
                .posY(s.getPosY())
                .status(s.getStatus())
                .rowNumber(s.getRowNumber())
                .seatNumber(s.getSeatNumber())
                .tableNumber(s.getTableNumber())
                .reservedByFullName(s.getReservedBy() != null ? s.getReservedBy().getFullName() : null)
                .build();
    }
}
