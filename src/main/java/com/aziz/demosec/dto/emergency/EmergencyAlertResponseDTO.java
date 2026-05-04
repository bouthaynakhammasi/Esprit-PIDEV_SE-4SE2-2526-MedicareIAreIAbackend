package com.aziz.demosec.dto.emergency;

import com.aziz.demosec.Entities.EmergencyAlertStatus;
import com.aziz.demosec.Entities.EmergencySeverity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyAlertResponseDTO {
    private Long id;
    private Long smartDeviceId;
    private String patientName;
    private EmergencySeverity severity;
    private EmergencyAlertStatus status;
    private Double latitude;
    private Double longitude;
    private Boolean canceledByPatient;
    private LocalDateTime createdAt;

}
