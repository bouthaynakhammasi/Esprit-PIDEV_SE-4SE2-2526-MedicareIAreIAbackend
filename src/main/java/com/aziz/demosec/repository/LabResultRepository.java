package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.LabResult;
import com.aziz.demosec.dto.LabStaffPerformanceDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LabResultRepository extends JpaRepository<LabResult, Long> {
    Optional<LabResult> findByLabRequestId(Long labRequestId);
    boolean existsByLabRequestId(Long labRequestId);
    List<LabResult> findByLabRequest_Laboratory_Id(Long laboratoryId);

    @Query("""
            SELECT new com.aziz.demosec.dto.LabStaffPerformanceDTO(
                r.technicianName,
                COUNT(r),
                SUM(CASE WHEN r.aiRisk = 'URGENT'        THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.aiRisk = 'ATTENTION'     THEN 1 ELSE 0 END),
                SUM(CASE WHEN r.aiRisk = 'SURVEILLANCE'  THEN 1 ELSE 0 END),
                ROUND(
                    SUM(CASE WHEN r.aiRisk = 'URGENT' THEN 1.0 ELSE 0.0 END)
                    / NULLIF(COUNT(r), 0) * 100
                , 1)
            )
            FROM LabResult r
            WHERE r.technicianName IS NOT NULL
              AND r.completedAt BETWEEN :from AND :to
            GROUP BY r.technicianName
            ORDER BY COUNT(r) DESC
            """)
    List<LabStaffPerformanceDTO> findStaffPerformance(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    @Query("""
            SELECT COUNT(r)
            FROM LabResult r
            WHERE r.completedAt BETWEEN :from AND :to
            """)
    Long countAnalysesBetween(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    @Query("""
            SELECT COUNT(r)
            FROM LabResult r
            WHERE r.aiRisk = 'URGENT'
              AND r.completedAt BETWEEN :from AND :to
            """)
    Long countUrgentBetween(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    @Query("""
            SELECT r FROM LabResult r
            JOIN FETCH r.labRequest req
            JOIN FETCH req.patient p
            JOIN FETCH req.laboratory lab
            LEFT JOIN FETCH req.doctor d
            WHERE r.aiRisk IN ('URGENT', 'ATTENTION')
              AND r.completedAt >= :since
              AND NOT EXISTS (
                  SELECT lr FROM LabRequest lr
                  WHERE lr.patient.id = p.id
                    AND lr.requestedAt > r.completedAt
                    AND lr.testType = 'Alzheimer Follow-up'
              )
            """)
    List<LabResult> findAtRiskWithoutFollowUp(@Param("since") LocalDateTime since);

    // ────────────────────────────────────────────────────────────────
    // AI NARRATOR — JPQL
    // Finds all completed lab results that have not yet received an
    // AI-generated narrative (narrativeGenerated = false).
    // Uses JOIN FETCH to load patient and doctor in a single SQL query
    // (avoids N+1 problem). Only processes results from the last 7 days.
    // Called by: LabNarratorScheduler (every 3 days) + LabNarratorController (manual trigger)
    // ────────────────────────────────────────────────────────────────
    @Query("""
            SELECT r FROM LabResult r
            JOIN FETCH r.labRequest req
            JOIN FETCH req.patient p
            LEFT JOIN FETCH req.doctor d
            WHERE r.narrativeGenerated = false
              AND r.aiRisk IS NOT NULL
              AND r.status = 'COMPLETED'
              AND r.completedAt >= :since
            ORDER BY r.completedAt DESC
            """)
    List<LabResult> findResultsNeedingNarrative(@Param("since") LocalDateTime since);

    // ────────────────────────────────────────────────────────────────
    // AI NARRATOR — KEYWORD 1
    // Retrieves the full analysis history for a given patient,
    // ordered from most recent to oldest.
    // Spring Data generates the SQL automatically from the method name:
    // LabResult → labRequest → patient → id + ORDER BY completedAt DESC
    // Used to build the longitudinal history section in the GPT prompt.
    // ────────────────────────────────────────────────────────────────
    List<LabResult> findByLabRequest_Patient_IdOrderByCompletedAtDesc(Long patientId);

    // ────────────────────────────────────────────────────────────────
    // AI NARRATOR — KEYWORD 2
    // Counts how many URGENT results a patient has had in total.
    // Spring Data traverses: LabResult → labRequest → patient → id
    //                    AND: LabResult → aiRisk
    // This count is injected into the GPT prompt to give the LLM
    // context about the patient's risk history severity.
    // ────────────────────────────────────────────────────────────────
    Long countByLabRequest_Patient_IdAndAiRisk(Long patientId, String aiRisk);

    // ────────────────────────────────────────────────────────────────
    // AI NARRATOR — KEYWORD 3
    // Retrieves all lab results for which a narrative was already
    // generated (narrativeGenerated = true), ordered by most recent.
    // Used by GET /api/lab-narrator/history to display the history
    // modal in the lab staff dashboard.
    // ────────────────────────────────────────────────────────────────
    List<LabResult> findByNarrativeGeneratedTrueOrderByCompletedAtDesc();
}
