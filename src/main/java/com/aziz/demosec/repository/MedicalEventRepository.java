package com.aziz.demosec.repository;

import com.aziz.demosec.dto.EventStatsResponse;
import com.aziz.demosec.Entities.MedicalEvent;
import com.aziz.demosec.Entities.MedicalEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MedicalEventRepository extends JpaRepository<MedicalEvent, Long> {
    List<MedicalEvent> findByEventType(MedicalEventType type);
    List<MedicalEvent> findByDateAfterOrderByDateAsc(LocalDateTime date);

    @Query("SELECT new com.aziz.demosec.dto.EventStatsResponse(" +
           "me.title, 'EVENT', " +
           "COUNT(ep), " +
           "COALESCE(SUM(CASE WHEN ep.status = 'PENDING' THEN 1L ELSE 0L END), 0L), " +
           "CAST(me.capacity - COALESCE(SUM(CASE WHEN ep.status = 'CONFIRMED' THEN 1L ELSE 0L END), 0L) AS Integer)) " +
           "FROM MedicalEvent me " +
           "LEFT JOIN EventParticipation ep ON ep.event = me " +
           "GROUP BY me.id, me.title, me.eventType, me.capacity")
    List<EventStatsResponse> getEventStats();

    @Query("SELECT me FROM MedicalEvent me " +
           "WHERE LOWER(me.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(me.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<MedicalEvent> searchEvents(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT me FROM MedicalEvent me WHERE me.date < :currentDate ORDER BY me.date DESC")
    List<MedicalEvent> findPreviousEvents(@Param("currentDate") LocalDateTime currentDate, Pageable pageable);
}