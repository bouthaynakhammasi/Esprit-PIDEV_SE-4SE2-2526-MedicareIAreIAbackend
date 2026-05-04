package com.aziz.demosec.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private String title;
    private String message;
    private String type;
    @JsonProperty("isRead")
    private boolean isRead;
    private LocalDateTime createdAt;
    private Long relatedId;
    private String meetLink;
}
