package com.aziz.demosec.controller;

import com.aziz.demosec.dto.EventSuggestionRequest;
import com.aziz.demosec.Entities.EventSuggestion;
import com.aziz.demosec.service.EventSuggestionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-suggestions")
@RequiredArgsConstructor
public class EventSuggestionController {

    private final EventSuggestionServiceImpl suggestionService;

    @PostMapping
    public ResponseEntity<EventSuggestion> suggest(
            @RequestBody EventSuggestionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(suggestionService.suggestEvent(request, userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<List<EventSuggestion>> getAll() {
        return ResponseEntity.ok(suggestionService.getAllSuggestions());
    }
}
