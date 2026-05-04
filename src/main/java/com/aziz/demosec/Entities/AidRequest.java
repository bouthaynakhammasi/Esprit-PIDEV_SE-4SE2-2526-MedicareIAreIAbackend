package com.aziz.demosec.Entities;

import com.aziz.demosec.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "aid_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AidRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Column(nullable = false)
    private String description;
    
    @Lob
    @Column(name="document_file", columnDefinition="LONGTEXT")
    private String documentFile;

    // ─── Données AI Éligibilité ───────────────────────────────────────────────
    private String chronicDiseases;        // NONE | DIABETES | HYPERTENSION | ...
    private Integer hereditaryDiseases;    // 0 ou 1
    private Integer drugAllergies;         // 0 ou 1
    private String diagnosisType;          // NONE | ACUTE | CHRONIC | TERMINAL
    private Integer nbDiagnoses;
    private Integer nbPrescriptions;
    private Double revenusMenuelsTnd;
    private Integer personnesACharge;
    private String situationProfessionnelle; // EMPLOYED | UNEMPLOYED | ...
    private Double scorePrecarite;
    // ─────────────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private AidRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AidRequestStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


}