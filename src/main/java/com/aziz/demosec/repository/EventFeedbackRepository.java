package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.EventFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventFeedbackRepository extends JpaRepository<EventFeedback, Long> {
    List<EventFeedback> findByEventId(Long eventId);
    
    @Query("SELECT AVG(f.rating) FROM EventFeedback f WHERE f.event.id = :eventId")
    Double getAverageRating(@Param("eventId") Long eventId);
    
    Optional<EventFeedback> findByEventIdAndUserId(Long eventId, Long userId);
}
