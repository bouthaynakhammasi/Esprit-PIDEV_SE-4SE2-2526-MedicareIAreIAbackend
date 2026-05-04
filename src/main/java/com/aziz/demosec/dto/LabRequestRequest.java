package com.aziz.demosec.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LabRequestRequest {

    // ─── PATIENT ───────────────────────────────
    @NotNull(message = "Patient ID is required")
    @Positive(message = "Patient ID must be a positive number")
    private Long patientId;

    // ─── DOCTOR (optionnel) ────────────────────
    @Positive(message = "Doctor ID must be a positive number")
    private Long doctorId;

    // ─── LABORATOIRE ───────────────────────────
    @NotNull(message = "Laboratory ID is required")
    @Positive(message = "Laboratory ID must be a positive number")
    private Long laboratoryId;

    // ─── QUI A FAIT LA DEMANDE ─────────────────
    // optionnel — juste pour info
    private RequestedBy requestedBy;

    // ─── TYPE DE TEST ──────────────────────────
    @NotBlank(message = "Test type is required")
    @Size(min = 3, max = 100, message = "Test type must be between 3 and 100 characters")
    private String testType;

    // ─── NOTES CLINIQUES ───────────────────────
    @Size(max = 1000, message = "Clinical notes must not exceed 1000 characters")
    private String clinicalNotes;

    // ─── DATE SOUHAITÉE ────────────────────────
    @Future(message = "Scheduled date must be in the future")
    private LocalDateTime scheduledAt;

    @Email(message = "Doctor email must be a valid email address")
    private String doctorEmail;
}