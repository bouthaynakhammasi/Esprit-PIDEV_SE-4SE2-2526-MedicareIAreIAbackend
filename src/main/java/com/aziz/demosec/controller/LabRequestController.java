package com.aziz.demosec.controller;

import com.aziz.demosec.Entities.LabRequestStatus;
import com.aziz.demosec.dto.LabRequestRequest;
import com.aziz.demosec.dto.LabRequestResponse;
import com.aziz.demosec.dto.RequestedBy;
import com.aziz.demosec.service.LabRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/lab-requests")
@RequiredArgsConstructor
@Slf4j
public class LabRequestController {

    private final LabRequestService labRequestService;

    // ═══════════════════════════════════════════════════════════
    // MÉTHODES EXISTANTES (inchangées)
    // ═══════════════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody LabRequestRequest request) {
        try {
            log.info("💾 CRÉATION LAB REQUEST - Requête reçue: {}", request);
            LabRequestResponse response = labRequestService.create(request);
            log.info("✅ LAB REQUEST CRÉÉE - ID: {}, Patient: {}, Labo: {}",
                    response.getId(), response.getPatientId(), response.getLaboratoryId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("❌ ERREUR CRÉATION LAB REQUEST: {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabRequestResponse> getById(@PathVariable Long id) {
        log.info("RECHERCHE LAB REQUEST PAR ID - ID: {}", id);
        LabRequestResponse response = labRequestService.getById(id);
        log.info("LAB REQUEST TROUVÉE - ID: {}, Status: {}", response.getId(), response.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<LabRequestResponse>> getAll() {
        log.info("RECHERCHE TOUTES LES LAB REQUESTS");
        List<LabRequestResponse> responses = labRequestService.getAll();
        log.info("RÉSULTAT - {} demandes trouvées", responses.size());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LabRequestResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LabRequestRequest request) {
        log.info("MISE À JOUR LAB REQUEST - ID: {}, Requête: {}", id, request);
        LabRequestResponse response = labRequestService.update(id, request);
        log.info("LAB REQUEST MODIFIÉE - ID: {}, Nouveau statut: {}", response.getId(), response.getStatus());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("SUPPRESSION LAB REQUEST - ID: {}", id);
        labRequestService.delete(id);
        log.info("LAB REQUEST SUPPRIMÉE - ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<LabRequestResponse>> getByPatient(
            @PathVariable Long patientId) {
        log.info("RECHERCHE LAB REQUESTS PAR PATIENT - Patient ID: {}", patientId);
        List<LabRequestResponse> requests = labRequestService.getByPatient(patientId);
        log.info("RÉSULTAT - {} demandes trouvées pour le patient {}", requests.size(), patientId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/patient/{patientId}/history")
    public ResponseEntity<List<LabRequestResponse>> getPatientHistory(
            @PathVariable Long patientId) {
        log.info("RECHERCHE HISTORIQUE - Patient ID: {}", patientId);
        List<LabRequestResponse> requests = labRequestService.getPatientHistory(patientId);
        log.info("RÉSULTAT - {} demandes pour le patient {}", requests.size(), patientId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<LabRequestResponse>> getByDoctor(
            @PathVariable Long doctorId) {
        return ResponseEntity.ok(labRequestService.getByDoctor(doctorId));
    }

    @GetMapping("/laboratory/{laboratoryId}")
    public ResponseEntity<List<LabRequestResponse>> getByLaboratory(
            @PathVariable Long laboratoryId) {
        return ResponseEntity.ok(labRequestService.getByLaboratory(laboratoryId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LabRequestResponse>> getByStatus(
            @PathVariable LabRequestStatus status) {
        return ResponseEntity.ok(labRequestService.getByStatus(status));
    }

    @GetMapping("/requested-by/{requestedBy}")
    public ResponseEntity<List<LabRequestResponse>> getByRequestedBy(
            @PathVariable RequestedBy requestedBy) {
        return ResponseEntity.ok(labRequestService.getByRequestedBy(requestedBy));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<LabRequestResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam LabRequestStatus status) {
        return ResponseEntity.ok(labRequestService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<LabRequestResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(labRequestService.cancel(id));
    }

    @PatchMapping("/{id}/notify")
    public ResponseEntity<LabRequestResponse> markNotificationSent(
            @PathVariable Long id) {
        return ResponseEntity.ok(labRequestService.markNotificationSent(id));
    }

    @GetMapping("/laboratory/{laboratoryId}/pending")
    public ResponseEntity<List<LabRequestResponse>> getPendingByLaboratory(
            @PathVariable Long laboratoryId) {
        return ResponseEntity.ok(
                labRequestService.getPendingByLaboratory(laboratoryId));
    }

    // ═══════════════════════════════════════════════════════════
    // TÂCHE 2 — JPQL AVEC JOIN FETCH
    // GET /api/lab-requests/laboratory/{id}/details?status=PENDING
    // Jointure sur 3 tables : LabRequest + Patient + Laboratory + Doctor
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/laboratory/{laboratoryId}/details")
    public ResponseEntity<List<LabRequestResponse>> getByLaboratoryWithDetails(
            @PathVariable Long laboratoryId,
            @RequestParam LabRequestStatus status) {

        log.info("📋 [JPQL JOIN] Labo: {}, Status: {}", laboratoryId, status);
        return ResponseEntity.ok(
                labRequestService.getByLaboratoryAndStatusWithDetails(laboratoryId, status));
    }

    // ═══════════════════════════════════════════════════════════
    // TÂCHE 3 — KEYWORDS MULTI-TABLES
    // ═══════════════════════════════════════════════════════════

    /**
     * Recherche par nom de patient + statut
     * GET /api/lab-requests/search/by-patient-name?lastName=Ben&status=PENDING
     * Spring Data traverse : lab_request JOIN patient
     */
    @GetMapping("/search/by-patient-name")
    public ResponseEntity<List<LabRequestResponse>> searchByPatientName(
            @RequestParam String lastName,
            @RequestParam LabRequestStatus status) {

        log.info("🔍 [KEYWORD] Patient: '{}', Status: {}", lastName, status);
        return ResponseEntity.ok(
                labRequestService.searchByPatientLastName(lastName, status));
    }

    /**
     * Recherche par nom de labo + statut + type de test
     * GET /api/lab-requests/search/by-lab?labName=Bio&status=PENDING&testType=BLOOD
     * Spring Data traverse : lab_request JOIN laboratory
     */
    @GetMapping("/search/by-lab")
    public ResponseEntity<List<LabRequestResponse>> searchByLab(
            @RequestParam String labName,
            @RequestParam LabRequestStatus status,
            @RequestParam String testType) {

        log.info("🔍 [KEYWORD] Labo: '{}', Status: {}, Test: {}", labName, status, testType);
        return ResponseEntity.ok(
                labRequestService.searchByLaboratoryName(labName, status, testType));
    }
}