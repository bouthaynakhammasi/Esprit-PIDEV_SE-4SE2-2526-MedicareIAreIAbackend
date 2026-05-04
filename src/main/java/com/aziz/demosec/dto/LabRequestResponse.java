package com.aziz.demosec.dto;

import com.aziz.demosec.Entities.LabRequestStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LabRequestResponse {

    private Long id;

    private Long patientId;
    private String patientName;

    private Long doctorId;
    private String doctorName;

    private Long laboratoryId;
    private String laboratoryName;

    private RequestedBy requestedBy;
    private LabRequestStatus status;
    private String testType;
    private String clinicalNotes;
    private String doctorEmail;

    private LocalDateTime scheduledAt;
    private LocalDateTime requestedAt;

    private boolean notificationSent;
    private boolean hasResult;
}