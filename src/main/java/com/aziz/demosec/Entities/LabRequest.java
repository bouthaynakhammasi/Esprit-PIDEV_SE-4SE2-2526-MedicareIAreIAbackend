package com.aziz.demosec.Entities;

import com.aziz.demosec.dto.RequestedBy;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LabRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "laboratory_id", nullable = false)
    private Laboratory laboratory;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RequestedBy requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LabRequestStatus status;

    @Column(nullable = false)
    private String testType;

    @Column(columnDefinition = "TEXT")
    private String clinicalNotes;


    // Email du médecin pour l'envoi du rapport IA
    @Column(length = 255)
    private String doctorEmail;

    // Date souhaitée pour le test

    private LocalDateTime scheduledAt;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    @Builder.Default
    private boolean notificationSent = false;

    @OneToOne(mappedBy = "labRequest", cascade = CascadeType.ALL)
    private LabResult labResult;
}