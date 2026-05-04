package com.aziz.demosec.dto.homecare;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResultDTO {
    private Long requestId;
    private Long assignedProviderId;
    private String providerName;
    private double providerRating;
    private int currentWorkload;
    private LocalDateTime assignedAt;
}
