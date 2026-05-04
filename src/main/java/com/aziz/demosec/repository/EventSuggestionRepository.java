package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.EventSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSuggestionRepository extends JpaRepository<EventSuggestion, Long> {
}
