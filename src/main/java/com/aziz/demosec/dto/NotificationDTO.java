package com.aziz.demosec.dto;

import com.aziz.demosec.Entities.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private Long targetId;
    private Long participationId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String participationStatus;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String senderName;
}
