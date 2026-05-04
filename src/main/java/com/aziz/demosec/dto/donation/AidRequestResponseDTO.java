package com.aziz.demosec.dto.donation;

import com.aziz.demosec.Entities.AidRequestStatus;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AidRequestResponseDTO {
    private Long id;
    private Long patientId;
    private String patientName;
    private String description;
    private String supportingDocument;
    private AidRequestStatus status;
    private LocalDateTime createdAt;
}