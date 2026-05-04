package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.LabRequest;
import com.aziz.demosec.Entities.LabRequestStatus;
import com.aziz.demosec.dto.RequestedBy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LabRequestRepository extends JpaRepository<LabRequest, Long> {

    // ═══════════════════════════════════════════════════════════
    // MÉTHODES EXISTANTES
    // ═══════════════════════════════════════════════════════════

    List<LabRequest> findByPatientId(Long patientId);
    List<LabRequest> findByDoctorId(Long doctorId);
    List<LabRequest> findByLaboratoryId(Long laboratoryId);
    List<LabRequest> findByStatus(LabRequestStatus status);
    List<LabRequest> findByRequestedBy(RequestedBy requestedBy);
    List<LabRequest> findByPatientIdOrderByRequestedAtDesc(Long patientId);
    List<LabRequest> findByStatusAndNotificationSentFalse(LabRequestStatus status);
    List<LabRequest> findByLaboratoryIdAndStatus(Long laboratoryId, LabRequestStatus status);

    // ═══════════════════════════════════════════════════════════
    // TÂCHE 1 — SCHEDULER
    // ═══════════════════════════════════════════════════════════

    // KEYWORD — compte les PENDING expirés avant cutoff
    long countByStatusAndRequestedAtBefore(
            LabRequestStatus status,
            LocalDateTime cutoff);

    // JPQL @Modifying — UPDATE en masse PENDING → CANCELLED
    @Modifying
    @Query("""
        UPDATE LabRequest lr
        SET lr.status = :newStatus
        WHERE lr.status = :oldStatus
        AND lr.requestedAt < :cutoff
    """)
    int cancelExpiredRequests(
            @Param("cutoff")    LocalDateTime cutoff,
            @Param("oldStatus") LabRequestStatus oldStatus,
            @Param("newStatus") LabRequestStatus newStatus);

    // ═══════════════════════════════════════════════════════════
    // TÂCHE 2 — JPQL AVEC JOIN FETCH (3 tables)
    // LabRequest JOIN Patient JOIN Laboratory LEFT JOIN Doctor
    // ═══════════════════════════════════════════════════════════

    @Query("""
        SELECT lr FROM LabRequest lr
        JOIN FETCH lr.patient p
        JOIN FETCH lr.laboratory lab
        LEFT JOIN FETCH lr.doctor d
        WHERE lab.id = :laboratoryId
        AND lr.status = :status
        ORDER BY lr.requestedAt DESC
    """)
    List<LabRequest> findByLaboratoryAndStatusWithDetails(
            @Param("laboratoryId") Long laboratoryId,
            @Param("status")       LabRequestStatus status);

    // ═══════════════════════════════════════════════════════════
    // TÂCHE 3 — MULTI-TABLES
    // ✅ Patient extends User → le champ est fullName (dans User)
    //    Keyword impossible → JPQL obligatoire
    // ✅ Laboratory entité directe → Keyword fonctionne
    // ═══════════════════════════════════════════════════════════

    // JPQL — traverse LabRequest → Patient(User) sur fullName
    @Query("""
        SELECT lr FROM LabRequest lr
        JOIN lr.patient p
        WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))
        AND lr.status = :status
    """)
    List<LabRequest> findByPatientFullNameAndStatus(
            @Param("fullName") String fullName,
            @Param("status")   LabRequestStatus status);

    // KEYWORD — traverse LabRequest → Laboratory sur name + status + testType
    List<LabRequest> findByLaboratory_NameContainingIgnoreCaseAndStatusAndTestType(
            String labName,
            LabRequestStatus status,
            String testType);
}