package com.aziz.demosec.dto.donation;

import com.aziz.demosec.Entities.AidRequestType;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AidRequestDTO {

    @NotNull(message = "L'ID du patient est obligatoire")
    private Long patientId;

    private AidRequestType type;

    @NotBlank(message = "La description de la demande d'aide est requise")
    @Size(min = 10, max = 500, message = "La description doit comporter entre 10 et 500 caractères")
    private String description;

    private String supportingDocument;

    // ─── Données AI Éligibilité (formulaire patient) ──────────────────────────
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
