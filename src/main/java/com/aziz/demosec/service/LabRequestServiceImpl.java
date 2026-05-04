package com.aziz.demosec.service;

import com.aziz.demosec.Entities.*;
import com.aziz.demosec.dto.LabRequestRequest;
import com.aziz.demosec.dto.LabRequestResponse;
import com.aziz.demosec.dto.RequestedBy;
import com.aziz.demosec.exception.ResourceNotFoundException;
import com.aziz.demosec.Mapper.LabRequestMapper;
import com.aziz.demosec.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabRequestServiceImpl implements LabRequestService {

    private final LabRequestRepository labRequestRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final LabRequestMapper labRequestMapper;

    @Override
    public LabRequestResponse create(LabRequestRequest request) {
        log.info("💾 CRÉATION EN BASE - Patient ID: {}, Labo ID: {}, Test: {}",
                request.getPatientId(), request.getLaboratoryId(), request.getTestType());

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient not found with id: " + request.getPatientId()));

        Laboratory laboratory = laboratoryRepository.findById(request.getLaboratoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Laboratory not found with id: " + request.getLaboratoryId()));

        if (!laboratory.isActive()) {
            log.error("❌ LABORATOIRE INACTIF - {}", laboratory.getName());
            throw new IllegalArgumentException(
                    "Laboratory '" + laboratory.getName() + "' is currently inactive");
        }

        Doctor doctor = null;
        if (request.getDoctorId() != null) {
            doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Doctor not found with id: " + request.getDoctorId()));
        }

        LabRequest labRequest = labRequestMapper.toEntity(request);
        labRequest.setPatient(patient);
        labRequest.setDoctor(doctor);
        labRequest.setLaboratory(laboratory);
        labRequest.setStatus(LabRequestStatus.PENDING);
        labRequest.setRequestedAt(LocalDateTime.now());

        LabRequest savedRequest = labRequestRepository.save(labRequest);
        log.info("✅ SAUVEGARDE EN BASE - ID: {}, Status: {}",
                savedRequest.getId(), savedRequest.getStatus());

        return labRequestMapper.toDto(savedRequest);
    }

    @Override
    public LabRequestResponse getById(Long id) {
        log.info("🔍 REQUÊTE BASE - ID: {}", id);
        LabRequest labRequest = findOrThrow(id);
        log.info("📋 DEMANDE TROUVÉE - ID: {}, Status: {}", id, labRequest.getStatus());
        return labRequestMapper.toDto(labRequest);
    }

    @Override
    public List<LabRequestResponse> getAll() {
        log.info("🔍 REQUÊTE BASE - Toutes les demandes");
        List<LabRequest> requests = labRequestRepository.findAll();
        log.info("📊 RÉSULTAT BASE - {} demandes trouvées", requests.size());
        return requests.stream()
                .map(request -> {
                    log.debug("📋 Demande - ID: {}, Status: {}, Test: {}",
                            request.getId(), request.getStatus(), request.getTestType());
                    return labRequestMapper.toDto(request);
                })
                .collect(Collectors.toList());
    }

    @Override
    public LabRequestResponse update(Long id, LabRequestRequest request) {
        log.info("📝 MISE À JOUR BASE - ID: {}, Nouvelles données: {}", id, request);

        LabRequest labRequest = findOrThrow(id);
        log.info("📋 ÉTAT ACTUEL - ID: {}, Status: {}", id, labRequest.getStatus());

        if (labRequest.getStatus() == LabRequestStatus.COMPLETED ||
                labRequest.getStatus() == LabRequestStatus.CANCELLED) {
            log.error("❌ MISE À JOUR IMPOSSIBLE - Statut final: {}", labRequest.getStatus());
            throw new IllegalArgumentException(
                    "Cannot update a request with status: " + labRequest.getStatus());
        }

        labRequestMapper.updateFromDto(request, labRequest);
        LabRequest updatedRequest = labRequestRepository.save(labRequest);
        log.info("✅ MISE À JOUR EFFECTUÉE - ID: {}, Nouveau statut: {}",
                updatedRequest.getId(), updatedRequest.getStatus());

        return labRequestMapper.toDto(updatedRequest);
    }

    @Override
    public void delete(Long id) {
        log.info("🗑️ SUPPRESSION BASE - ID: {}", id);

        LabRequest labRequest = findOrThrow(id);
        log.info("📋 DEMANDE À SUPPRIMER - ID: {}, Status: {}", id, labRequest.getStatus());

        if (labRequest.getStatus() == LabRequestStatus.COMPLETED) {
            log.error("❌ SUPPRESSION IMPOSSIBLE - Demande complétée: {}", labRequest.getStatus());
            throw new IllegalArgumentException("Cannot delete a COMPLETED lab request.");
        }

        labRequestRepository.deleteById(id);
        log.info("✅ SUPPRESSION EFFECTUÉE - ID: {}", id);
    }

    @Override
    public List<LabRequestResponse> getByPatient(Long patientId) {
        log.info("🔍 REQUÊTE BASE - Patient ID: {}", patientId);
        List<LabRequest> requests = labRequestRepository.findByPatientId(patientId);
        log.info("📊 RÉSULTAT BASE - {} demandes pour patient {}", requests.size(), patientId);
        return requests.stream()
                .map(labRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LabRequestResponse> getByDoctor(Long doctorId) {
        log.info("🔍 REQUÊTE BASE - Doctor ID: {}", doctorId);
        List<LabRequest> requests = labRequestRepository.findByDoctorId(doctorId);
        log.info("📊 RÉSULTAT BASE - {} demandes pour doctor {}", requests.size(), doctorId);
        return requests.stream()
                .map(labRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LabRequestResponse> getByLaboratory(Long laboratoryId) {
        return labRequestRepository.findByLaboratoryId(laboratoryId)
                .stream().map(labRequestMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<LabRequestResponse> getByStatus(LabRequestStatus status) {
        return labRequestRepository.findByStatus(status)
                .stream().map(labRequestMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<LabRequestResponse> getByRequestedBy(RequestedBy requestedBy) {
        return labRequestRepository.findByRequestedBy(requestedBy)
                .stream().map(labRequestMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<LabRequestResponse> getPatientHistory(Long patientId) {
        return labRequestRepository.findByPatientIdOrderByRequestedAtDesc(patientId)
                .stream().map(labRequestMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public LabRequestResponse updateStatus(Long id, LabRequestStatus status) {
        LabRequest labRequest = findOrThrow(id);
        validateStatusTransition(labRequest.getStatus(), status);
        labRequest.setStatus(status);
        return labRequestMapper.toDto(labRequestRepository.save(labRequest));
    }

    @Override
    public LabRequestResponse cancel(Long id) {
        LabRequest labRequest = findOrThrow(id);
        if (labRequest.getStatus() == LabRequestStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel a COMPLETED lab request.");
        }
        labRequest.setStatus(LabRequestStatus.CANCELLED);
        return labRequestMapper.toDto(labRequestRepository.save(labRequest));
    }

    @Override
    public LabRequestResponse markNotificationSent(Long id) {
        LabRequest labRequest = findOrThrow(id);
        if (labRequest.getStatus() != LabRequestStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Notification can only be sent for COMPLETED requests.");
        }
        labRequest.setNotificationSent(true);
        return labRequestMapper.toDto(labRequestRepository.save(labRequest));
    }

    @Override
    public List<LabRequestResponse> getPendingByLaboratory(Long laboratoryId) {
        return labRequestRepository
                .findByLaboratoryIdAndStatus(laboratoryId, LabRequestStatus.PENDING)
                .stream().map(labRequestMapper::toDto).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // TÂCHE 2 — JPQL AVEC JOIN FETCH (3 tables)
    // ═══════════════════════════════════════════════════════════

    @Override
    public List<LabRequestResponse> getByLaboratoryAndStatusWithDetails(
            Long laboratoryId, LabRequestStatus status) {

        log.info("🔍 [JPQL JOIN] Labo ID: {}, Status: {}", laboratoryId, status);

        List<LabRequest> requests = labRequestRepository
                .findByLaboratoryAndStatusWithDetails(laboratoryId, status);

        log.info("📊 [JPQL JOIN] {} demande(s) trouvée(s)", requests.size());

        return requests.stream()
                .map(labRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // TÂCHE 3 — MULTI-TABLES
    // ═══════════════════════════════════════════════════════════

    @Override
    public List<LabRequestResponse> searchByPatientLastName(
            String fullName, LabRequestStatus status) {

        log.info("🔍 [JPQL] Patient fullName: '{}', Status: {}", fullName, status);

        // ✅ CORRIGÉ : fullName car User.fullName (plus lastName)
        List<LabRequest> requests = labRequestRepository
                .findByPatientFullNameAndStatus(fullName, status);

        log.info("📊 [JPQL] {} demande(s) trouvée(s)", requests.size());

        return requests.stream()
                .map(labRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<LabRequestResponse> searchByLaboratoryName(
            String labName, LabRequestStatus status, String testType) {

        log.info("🔍 [KEYWORD] Labo name: '{}', Status: {}, TestType: {}",
                labName, status, testType);

        // ✅ KEYWORD fonctionne car Laboratory est une entité directe
        List<LabRequest> requests = labRequestRepository
                .findByLaboratory_NameContainingIgnoreCaseAndStatusAndTestType(
                        labName, status, testType);

        log.info("📊 [KEYWORD] {} demande(s) trouvée(s)", requests.size());

        return requests.stream()
                .map(labRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTHODES PRIVÉES
    // ═══════════════════════════════════════════════════════════

    private LabRequest findOrThrow(Long id) {
        return labRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LabRequest not found with id: " + id));
    }

    private void validateStatusTransition(LabRequestStatus current, LabRequestStatus next) {
        if (current == LabRequestStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Cannot change status of a COMPLETED request.");
        }
        if (current == LabRequestStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Cannot change status of a CANCELLED request.");
        }
    }
}