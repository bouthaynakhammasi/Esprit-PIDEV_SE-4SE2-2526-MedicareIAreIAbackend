package com.aziz.demosec.dto.homecare;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FastBookResultDTO {
    private Long requestId;
    private String providerName;
    private double providerRating;
    private String serviceName;
    private LocalDateTime requestedDateTime;
}
