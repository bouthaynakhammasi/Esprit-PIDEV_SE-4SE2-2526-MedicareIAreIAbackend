package com.aziz.demosec.dto.emergency;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoDispatchResponseDTO {
    private Long interventionId;
    private Long emergencyAlertId;
    private String patientName;
    private Long ambulanceId;
    private String ambulanceLicensePlate;
    private Long clinicId;
    private String clinicName;
    private double distanceKm;
    private String status;
    private LocalDateTime dispatchedAt;
}
