package com.aziz.demosec.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LabResultResponse {

    private Long id;
    private Long labRequestId;
    private String patientName;
    private String laboratoryName;
    private String testType;
    private String resultData;
    private String technicianName;
    private String verifiedBy;
    private String abnormalFindings;
    private String status;
    private Boolean isAbnormal;
    private LocalDateTime completedAt;
    private LocalDateTime verifiedAt;

    // Alzheimer AI
    private String aiDiagnostic;
    private String aiRisk;
    private Double aiConfidence;
    private Boolean aiAlertSent;
}