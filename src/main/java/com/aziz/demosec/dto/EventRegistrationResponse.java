package com.aziz.demosec.dto;

import com.aziz.demosec.Entities.RegistrationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventRegistrationResponse {
    private Long id;
    private Long eventId;
    private Long participantId;
    private RegistrationStatus status;
    private LocalDateTime createdAt;
}