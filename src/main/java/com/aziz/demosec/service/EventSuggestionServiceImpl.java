package com.aziz.demosec.service;

import com.aziz.demosec.Entities.NotificationType;
import com.aziz.demosec.domain.Role;
import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.EventSuggestionRequest;
import com.aziz.demosec.Entities.EventSuggestion;
import com.aziz.demosec.Entities.Notification;
import com.aziz.demosec.repository.EventSuggestionRepository;
import com.aziz.demosec.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSuggestionServiceImpl {

    private final EventSuggestionRepository suggestionRepository;
    private final UserRepository userRepository;
    private final NotificationServiceImpl notificationService;

    @Transactional
    public EventSuggestion suggestEvent(EventSuggestionRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        EventSuggestion suggestion = EventSuggestion.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .suggestedBy(user)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        EventSuggestion saved = suggestionRepository.save(suggestion);

        // Notify Admin
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            Notification notification = Notification.builder()
                    .recipient(admin)
                    .sender(user)
                    .title("New Event Suggestion")
                    .message("New Event Suggestion from " + user.getFullName() + ": " + suggestion.getTitle())
                    .type(NotificationType.EVENT_SUGGESTION)
                    .targetId(saved.getId())
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationService.sendNotification(notification);
        }

        return saved;
    }

    public List<EventSuggestion> getAllSuggestions() {
        return suggestionRepository.findAll();
    }
}
