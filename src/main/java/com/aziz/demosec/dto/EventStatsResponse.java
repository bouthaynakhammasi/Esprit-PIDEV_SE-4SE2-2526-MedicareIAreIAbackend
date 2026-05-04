package com.aziz.demosec.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventStatsResponse {
    private String title;
    private String eventType;
    private Long totalParticipants;
    private Long waitlistedCount;
    private Integer availableSpots;
}
