package com.aziz.demosec.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatChannelResponse {
    private Long id;
    private String name;
    private String description;
    private int unreadCount;
}
