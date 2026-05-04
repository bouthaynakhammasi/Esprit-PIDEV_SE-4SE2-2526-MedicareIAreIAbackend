package com.aziz.demosec.dto.donation;

import com.aziz.demosec.Entities.AidRequestStatus;
import com.aziz.demosec.Entities.AidRequestType;
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
    private AidRequestType type;
    private String description;
    private String supportingDocument;
    private AidRequestStatus status;
    private LocalDateTime createdAt;

    // ─── Données AI Éligibilité ───────────────────────────────────────────────
    private String chronicDiseases;
    private Integer hereditaryDiseases;
    private Integer drugAllergies;
    private String diagnosisType;
    private Integer nbDiagnoses;
    private Integer nbPrescriptions;
    private Double revenusMenuelsTnd;
    private Integer personnesACharge;
    private String situationProfessionnelle;
    private Double scorePrecarite;
    // ─────────────────────────────────────────────────────────────────────────
}
