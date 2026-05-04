package com.aziz.demosec.service;

import com.aziz.demosec.Entities.*;
import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.donation.*;
import com.aziz.demosec.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DonationServiceImpl implements IDonationService {

    private final DonationRepository donationRepository;
    private final AidRequestRepository aidRequestRepository;
    private final DonationAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final WsNotificationService wsNotificationService;
    private final EmailService emailService;

    // ─── DONATION ─────────────────────────────────────────────────

    @Override
    public DonationResponseDTO createDonation(DonationRequestDTO dto) {
        Donation donation = Donation.builder()
                .creatorId(dto.getCreatorId())
                .donorName(dto.getDonorName())
                .type(dto.getType())
                .status(DonationStatus.AVAILABLE)
                .amount(dto.getAmount())
                .categorie(dto.getCategorie())
                .description(dto.getDescription())
                .quantite(dto.getQuantite())
                .photoData(dto.getImageData())
                .build();
        return toResponseDTO(donationRepository.save(donation));
    }

    @Override
    public DonationResponseDTO getDonationById(Long id) {
        return toResponseDTO(findDonationById(id));
    }

    @Override
    public List<DonationResponseDTO> getAllDonations() {
        return donationRepository.findAll()
                .stream().map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DonationResponseDTO> getDonationsByStatus(DonationStatus status) {
        return donationRepository.findByStatus(status)
                .stream().map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DonationResponseDTO updateDonation(Long id, DonationRequestDTO dto) {
        Donation donation = findDonationById(id);
        donation.setDonorName(dto.getDonorName());
        donation.setAmount(dto.getAmount());
        donation.setCategorie(dto.getCategorie());
        donation.setDescription(dto.getDescription());
        donation.setQuantite(dto.getQuantite());
        donation.setPhotoData(dto.getImageData());
        return toResponseDTO(donationRepository.save(donation));
    }

    @Override
    public void deleteDonation(Long id) {
        donationRepository.deleteById(id);
    }

    // ─── AID REQUEST ──────────────────────────────────────────────

    @Override
    public AidRequestResponseDTO createAidRequest(AidRequestDTO dto) {
        User patient = userRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        AidRequest request = AidRequest.builder()
                .patient(patient)
                .type(dto.getType())
                .description(dto.getDescription())
                .documentFile(dto.getSupportingDocument())
                .status(AidRequestStatus.PENDING)
                // Données AI
                .chronicDiseases(dto.getChronicDiseases())
                .hereditaryDiseases(dto.getHereditaryDiseases())
                .drugAllergies(dto.getDrugAllergies())
                .diagnosisType(dto.getDiagnosisType())
                .nbDiagnoses(dto.getNbDiagnoses())
                .nbPrescriptions(dto.getNbPrescriptions())
                .revenusMenuelsTnd(dto.getRevenusMenuelsTnd())
                .personnesACharge(dto.getPersonnesACharge())
                .situationProfessionnelle(dto.getSituationProfessionnelle())
                .scorePrecarite(dto.getScorePrecarite())
                .build();

        AidRequestResponseDTO saved = toAidResponseDTO(aidRequestRepository.save(request));

        // 🔔 Notifier l'admin en temps réel via WebSocket
        wsNotificationService.notifyAdmin(
            "Nouvelle demande d'aide 🤝",
            patient.getFullName() + " a soumis une nouvelle demande d'aide.",
            "aid_request"
        );

        return saved;
    }

    @Override
    public AidRequestResponseDTO getAidRequestById(Long id) {
        return toAidResponseDTO(findAidRequestById(id));
    }

    @Override
    public List<AidRequestResponseDTO> getAllAidRequests() {
        return aidRequestRepository.findAll()
                .stream().map(this::toAidResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AidRequestResponseDTO> getAidRequestsByPatient(Long patientId) {
        return aidRequestRepository.findByPatientId(patientId)
                .stream().map(this::toAidResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AidRequestResponseDTO> getAidRequestsByStatus(AidRequestStatus status) {
        return aidRequestRepository.findByStatus(status)
                .stream().map(this::toAidResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AidRequestResponseDTO updateAidRequestStatus(Long id, AidRequestStatus status) {
        AidRequest request = findAidRequestById(id);
        request.setStatus(status);
        AidRequestResponseDTO result = toAidResponseDTO(aidRequestRepository.save(request));

        // 🔔 Notifier le patient selon le statut
        Long patientId = request.getPatient().getId();
        if (status == AidRequestStatus.APPROVED) {
            wsNotificationService.notifyPatient(patientId,
                "Demande approuvée ✅",
                "Bonne nouvelle ! Votre demande d'aide a été approuvée.",
                "info");
        } else if (status == AidRequestStatus.REJECTED) {
            wsNotificationService.notifyPatient(patientId,
                "Demande refusée ❌",
                "Votre demande d'aide n'a pas pu être approuvée cette fois.",
                "warning");
        }

        return result;
    }

    @Override
    public void deleteAidRequest(Long id) {
        aidRequestRepository.deleteById(id);
    }

    @Override
    public AidRequestResponseDTO updateAidRequest(Long id, AidRequestDTO dto) {
        AidRequest request = findAidRequestById(id);
        if (dto.getType() != null) request.setType(dto.getType());
        request.setDescription(dto.getDescription());
        if (dto.getSupportingDocument() != null && !dto.getSupportingDocument().isEmpty()) {
            request.setDocumentFile(dto.getSupportingDocument());
        }
        return toAidResponseDTO(aidRequestRepository.save(request));
    }

    // ─── ASSIGNMENT ───────────────────────────────────────────────

    @Override
    public DonationAssignmentResponseDTO assignDonation(DonationAssignmentDTO dto) {
        Donation donation = findDonationById(dto.getDonationId());
        AidRequest request = findAidRequestById(dto.getAidRequestId());

        donation.setStatus(DonationStatus.ASSIGNED);
        request.setStatus(AidRequestStatus.APPROVED);
        donationRepository.save(donation);
        aidRequestRepository.save(request);

        DonationAssignment assignment = DonationAssignment.builder()
                .donation(donation)
                .aidRequest(request)
                .build();

        DonationAssignment savedAssignment = assignmentRepository.save(assignment);
        DonationAssignmentResponseDTO result = toAssignmentResponseDTO(savedAssignment);

        // 🔔 Notifier le patient en temps réel via WebSocket
        wsNotificationService.notifyPatient(
            request.getPatient().getId(),
            "Don assigné 🎁",
            "Un don a été assigné à votre demande d'aide.",
            "info"
        );

        // 📧 Envoyer les emails de notification (asynchrone, n'impacte pas la transaction)
        assignmentRepository.findEmailDataByAssignmentId(savedAssignment.getId())
            .ifPresent(data -> emailService.sendAssignmentEmails(data, request.getId()));

        return result;
    }

    @Override
    public List<DonationAssignmentResponseDTO> getAllAssignments() {
        return assignmentRepository.findAll()
                .stream().map(this::toAssignmentResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DonationAssignmentResponseDTO> getAssignmentsByDonationId(Long donationId) {
        return assignmentRepository.findByDonationId(donationId)
                .stream().map(this::toAssignmentResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DonationAssignmentResponseDTO> getAssignmentsByAidRequestId(Long aidRequestId) {
        return assignmentRepository.findByAidRequestId(aidRequestId)
                .stream().map(this::toAssignmentResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── ADVANCED QUERIES ─────────────────────────────────────────

    @Override
    public List<DonationResponseDTO> getAvailableDonationsByCategory(String category) {
        return donationRepository.findAvailableDonationsByCategory(category)
                .stream().map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TopDonorDTO> getTopDonors() {
        return donationRepository.findTopDonorsByAssignmentCount();
    }

    @Override
    public List<DonationResponseDTO> getDonationsByPatientIdAndStatus(Long patientId, DonationStatus status) {
        return assignmentRepository.findDonationsByPatientIdAndStatus(patientId, status)
                .stream().map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ─── MAPPERS PRIVÉS ───────────────────────────────────────────

    private Donation findDonationById(Long id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Donation not found: " + id));
    }

    private AidRequest findAidRequestById(Long id) {
        return aidRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AidRequest not found: " + id));
    }

    private DonationResponseDTO toResponseDTO(Donation d) {
        String profileImg = null;
        if (d.getCreatorId() != null) {
            profileImg = userRepository.findById(d.getCreatorId())
                                       .map(user -> user.getProfileImage())
                                       .orElse(null);
        }

        return DonationResponseDTO.builder()
                .id(d.getId())
                .creatorId(d.getCreatorId())
                .donorName(d.getDonorName())
                .donorProfileImage(profileImg)
                .type(d.getType())
                .status(d.getStatus())
                .amount(d.getAmount())
                .categorie(d.getCategorie())
                .description(d.getDescription())
                .quantite(d.getQuantite())
                .imageData(d.getPhotoData())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private AidRequestResponseDTO toAidResponseDTO(AidRequest r) {
        return AidRequestResponseDTO.builder()
                .id(r.getId())
                .patientId(r.getPatient().getId())
                .patientName(r.getPatient().getFullName())
                .type(r.getType())
                .description(r.getDescription())
                .supportingDocument(r.getDocumentFile())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                // Données AI
                .chronicDiseases(r.getChronicDiseases())
                .hereditaryDiseases(r.getHereditaryDiseases())
                .drugAllergies(r.getDrugAllergies())
                .diagnosisType(r.getDiagnosisType())
                .nbDiagnoses(r.getNbDiagnoses())
                .nbPrescriptions(r.getNbPrescriptions())
                .revenusMenuelsTnd(r.getRevenusMenuelsTnd())
                .personnesACharge(r.getPersonnesACharge())
                .situationProfessionnelle(r.getSituationProfessionnelle())
                .scorePrecarite(r.getScorePrecarite())
                .build();
    }

    private DonationAssignmentResponseDTO toAssignmentResponseDTO(DonationAssignment a) {
        return DonationAssignmentResponseDTO.builder()
                .id(a.getId())
                .donationId(a.getDonation().getId())
                .aidRequestId(a.getAidRequest().getId())
                .patientName(a.getAidRequest().getPatient().getFullName())
                .assignedAt(a.getAssignedAt())
                .build();
    }
}