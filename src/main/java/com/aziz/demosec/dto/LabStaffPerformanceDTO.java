package com.aziz.demosec.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabStaffPerformanceDTO {
    private String technicianName;
    private Long   totalAnalyses;
    private Long   urgentCases;
    private Long   attentionCases;
    private Long   surveillanceCases;
    private Double urgentRate;
}
