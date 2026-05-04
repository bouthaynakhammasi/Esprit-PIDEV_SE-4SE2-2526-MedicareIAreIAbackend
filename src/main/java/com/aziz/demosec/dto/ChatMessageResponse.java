package com.aziz.demosec.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageResponse {
    private Long id;
    private Long channelId;
    private String content;
    private String authorName;
    private String authorRole;
    private LocalDateTime createdAt;
}
