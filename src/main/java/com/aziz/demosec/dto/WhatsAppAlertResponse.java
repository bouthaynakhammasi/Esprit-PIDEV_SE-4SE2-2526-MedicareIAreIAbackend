package com.aziz.demosec.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WhatsAppAlertResponse {
    private boolean sent;
    private int recipientCount;
    private String messageId;
}
