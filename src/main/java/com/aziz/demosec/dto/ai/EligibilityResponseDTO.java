package com.aziz.demosec.dto.ai;

import lombok.*;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityResponseDTO {

    private boolean eligible;
    private double probability;
    private String decision;
    private String confidence;
    private Map<String, Object> details;

    // Extra fields added by Spring Boot before returning to Angular
    private Long aidRequestId;
    private String patientName;

    // Per-medicine stock availability (MEDICAMENT requests only)
    private Map<String, Boolean> medicineAvailability;
}
