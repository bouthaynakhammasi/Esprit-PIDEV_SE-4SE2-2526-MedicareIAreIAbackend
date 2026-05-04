package com.aziz.demosec.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClinicalDataRequest {

    // ── Demographics ─────────────────────────────────────────
    private Double age;
    private Double gender;              // 0=Femme 1=Homme
    private Double ethnicity;           // 0-3
    private Double educationLevel;      // 0-3

    // ── Lifestyle ────────────────────────────────────────────
    private Double bmi;
    private Double smoking;             // 0/1
    private Double alcoholConsumption;  // litres/semaine
    private Double physicalActivity;    // heures/semaine
    private Double dietQuality;         // score 0-10
    private Double sleepQuality;        // score 4-10

    // ── Medical History ──────────────────────────────────────
    private Double familyHistoryAlzheimers; // 0/1
    private Double cardiovascularDisease;   // 0/1
    private Double diabetes;               // 0/1
    private Double depression;             // 0/1
    private Double headInjury;             // 0/1
    private Double hypertension;           // 0/1

    // ── Clinical Measurements ────────────────────────────────
    private Double systolicBP;              // mmHg
    private Double diastolicBP;             // mmHg
    private Double cholesterolTotal;        // mg/dL
    private Double cholesterolLDL;          // mg/dL
    private Double cholesterolHDL;          // mg/dL
    private Double cholesterolTriglycerides;// mg/dL

    // ── Cognitive Tests ──────────────────────────────────────
    private Double mmse;                   // 0-30
    private Double functionalAssessment;   // 0-10
    private Double adl;                    // 0-10

    // ── Symptoms ─────────────────────────────────────────────
    private Double memoryComplaints;       // 0/1
    private Double behavioralProblems;     // 0/1
    private Double confusion;             // 0/1
    private Double disorientation;        // 0/1
    private Double personalityChanges;    // 0/1
    private Double difficultyCompletingTasks; // 0/1
    private Double forgetfulness;         // 0/1

    // ── Optional override ────────────────────────────────────
    private String doctorEmail;
}
