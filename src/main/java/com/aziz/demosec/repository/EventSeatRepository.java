package com.aziz.demosec.repository;

import com.aziz.demosec.dto.SeatZoneSummaryResponse;
import com.aziz.demosec.Entities.EventSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventSeatRepository extends JpaRepository<EventSeat, Long> {

    List<EventSeat> findByEventId(Long eventId);

    // ── Zone-level summary (breakdown per zone/section/table group) ──────────
    @Query("SELECT new com.aziz.demosec.dto.SeatZoneSummaryResponse(" +
            "s.zoneName, COUNT(s.id), " +
            "SUM(CASE WHEN s.status = com.aziz.demosec.Entities.SeatStatus.AVAILABLE THEN 1L ELSE 0L END), " +
            "SUM(CASE WHEN s.status = com.aziz.demosec.Entities.SeatStatus.RESERVED  THEN 1L ELSE 0L END), " +
            "SUM(CASE WHEN s.status = com.aziz.demosec.Entities.SeatStatus.BLOCKED   THEN 1L ELSE 0L END)) " +
            "FROM EventSeat s WHERE s.event.id = :eventId GROUP BY s.zoneName")
    List<SeatZoneSummaryResponse> getSeatSummaryByZone(@Param("eventId") Long eventId);

    // ── Global aggregate: [total, available, reserved, blocked] ─────────────
    @Query("SELECT COUNT(s), " +
            "SUM(CASE WHEN s.status = com.aziz.demosec.Entities.SeatStatus.AVAILABLE THEN 1L ELSE 0L END), " +
            "SUM(CASE WHEN s.status = com.aziz.demosec.Entities.SeatStatus.RESERVED  THEN 1L ELSE 0L END), " +
            "SUM(CASE WHEN s.status = com.aziz.demosec.Entities.SeatStatus.BLOCKED   THEN 1L ELSE 0L END) " +
            "FROM EventSeat s LEFT JOIN s.event e WHERE e.id = :eventId")
    List<Object[]> getTotalSeatingStats(@Param("eventId") Long eventId);

    // ── Keyword search: zone name OR seat label (case-insensitive) ───────────
    @Query("SELECT s FROM EventSeat s LEFT JOIN s.event e " +
            "WHERE e.id = :eventId " +
            "AND (LOWER(s.zoneName)  LIKE LOWER(CONCAT('%', :kw, '%')) " +
            " OR  LOWER(s.seatLabel) LIKE LOWER(CONCAT('%', :kw, '%')))")
    List<EventSeat> searchSeats(@Param("eventId") Long eventId, @Param("kw") String kw);

    // ── Delete all seats for an event (supports layout regeneration) ─────────
    @Modifying
    @Query("DELETE FROM EventSeat s WHERE s.event.id = :eventId")
    void deleteAllByEventId(@Param("eventId") Long eventId);

    boolean existsByEventIdAndSeatLabel(Long eventId, String seatLabel);
}
