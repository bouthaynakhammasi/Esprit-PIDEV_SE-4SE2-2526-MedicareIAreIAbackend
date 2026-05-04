package com.aziz.demosec.service;

import com.aziz.demosec.Entities.AidRequest;
import com.aziz.demosec.Entities.AidRequestType;
import com.aziz.demosec.Entities.Donation;
import com.aziz.demosec.Entities.Patient;
import com.aziz.demosec.dto.ai.EligibilityRequestDTO;
import com.aziz.demosec.dto.ai.EligibilityResponseDTO;
import com.aziz.demosec.repository.AidRequestRepository;
import com.aziz.demosec.repository.DonationRepository;
import com.aziz.demosec.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EligibilityService {

    private final AidRequestRepository aidRequestRepository;
    private final PatientRepository    patientRepository;
    private final DonationRepository   donationRepository;
    private final RestTemplate         restTemplate;

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    /**
     * Récupère les données du patient lié à la demande d'aide,
     * construit la requête FastAPI et retourne le résultat de prédiction.
     */
    public EligibilityResponseDTO checkEligibility(Long aidRequestId) {

        // ── 1. Charger la demande d'aide ─────────────────────────────────────
        AidRequest aidRequest = aidRequestRepository.findById(aidRequestId)
                .orElseThrow(() -> new RuntimeException("AidRequest not found: " + aidRequestId));

        // ── Routing : type MEDICAMENT → modèle OCR prescription ─────────────
        if (aidRequest.getType() == AidRequestType.MEDICAMENT) {
            log.info("AidRequest#{} type=MEDICAMENT → using medicament OCR model", aidRequestId);
            return checkEligibilityByPrescription(aidRequest);
        }

        Long patientUserId = aidRequest.getPatient().getId();

        // ── 2. Charger le profil Patient ──────────────────────────────────────
        Patient patient = patientRepository.findById(patientUserId).orElse(null);

        // ── 3. Calculer l'âge depuis birthDate (format YYYY-MM-DD) ───────────
        Double age = null;
        if (patient != null && patient.getBirthDate() != null && !patient.getBirthDate().isBlank()) {
            try {
                LocalDate dob = LocalDate.parse(patient.getBirthDate(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                age = (double) Period.between(dob, LocalDate.now()).getYears();
            } catch (Exception e) {
                log.warn("Cannot parse birthDate '{}': {}", patient.getBirthDate(), e.getMessage());
            }
        }

        // ── 4. Compter les dons déjà reçus (via assignments) ─────────────────
        int nbDonsRecus = 0;
        // Utilise la description de la demande pour estimer la longueur
        int descLength = aidRequest.getDescription() != null ? aidRequest.getDescription().length() : 0;

        // ── 5. Construire le DTO vers FastAPI ─────────────────────────────────
        // Priorité : données saisies dans le formulaire AidRequest
        // Fallback  : données du profil Patient si formulaire vide

        String gender = "MALE";
        if (patient != null && patient.getGender() != null) {
            gender = patient.getGender().name();
        }

        // Chronic diseases : formulaire prioritaire, sinon profil patient
        String chronicDiseases = "NONE";
        if (aidRequest.getChronicDiseases() != null && !aidRequest.getChronicDiseases().isBlank()) {
            chronicDiseases = aidRequest.getChronicDiseases();
        } else if (patient != null && patient.getChronicDiseases() != null
                && !patient.getChronicDiseases().isBlank()) {
            chronicDiseases = mapChronicDisease(patient.getChronicDiseases());
        }

        int hereditaryDiseases = 0;
        if (aidRequest.getHereditaryDiseases() != null) {
            hereditaryDiseases = aidRequest.getHereditaryDiseases();
        } else if (patient != null && patient.getHereditaryDiseases() != null
                && !patient.getHereditaryDiseases().isBlank()) {
            hereditaryDiseases = 1;
        }

        int drugAllergies = 0;
        if (aidRequest.getDrugAllergies() != null) {
            drugAllergies = aidRequest.getDrugAllergies();
        } else if (patient != null && patient.getDrugAllergies() != null
                && !patient.getDrugAllergies().isBlank()) {
            drugAllergies = 1;
        }

        String diagnosisType = aidRequest.getDiagnosisType() != null
                ? aidRequest.getDiagnosisType() : mapDiagnosisType(chronicDiseases);

        int nbDiagnoses    = aidRequest.getNbDiagnoses()    != null ? aidRequest.getNbDiagnoses()    : 0;
        int nbPrescriptions = aidRequest.getNbPrescriptions() != null ? aidRequest.getNbPrescriptions() : 0;

        Double revenus = aidRequest.getRevenusMenuelsTnd(); // null → imputation médiane dans le modèle

        int personnesACharge = aidRequest.getPersonnesACharge() != null ? aidRequest.getPersonnesACharge() : 0;

        String sitPro = aidRequest.getSituationProfessionnelle() != null
                ? aidRequest.getSituationProfessionnelle() : "UNEMPLOYED";

        double scorePrecarite = aidRequest.getScorePrecarite() != null
                ? aidRequest.getScorePrecarite()
                : estimateScorePrecarite(chronicDiseases, hereditaryDiseases, drugAllergies, descLength);

        EligibilityRequestDTO aiRequest = EligibilityRequestDTO.builder()
                .age(age)
                .gender(gender)
                .chronicDiseases(chronicDiseases)
                .hereditaryDiseases(hereditaryDiseases)
                .drugAllergies(drugAllergies)
                .medicalRecordExists(patient != null ? 1 : 0)
                .nbDiagnoses(nbDiagnoses)
                .diagnosisType(diagnosisType)
                .nbPrescriptions(nbPrescriptions)
                .nbPrescriptionItems(nbPrescriptions * 2)
                .revenusMenuelsTnd(revenus)
                .personnesACharge(personnesACharge)
                .situationProfessionnelle(sitPro)
                .scorePrecarite(scorePrecarite)
                .nbDonsRecus(nbDonsRecus)
                .montantDonsTnd(0.0)
                .donationStatus("AVAILABLE")
                .build();

        // ── 6. Appel FastAPI ──────────────────────────────────────────────────
        String endpoint = aiServiceUrl + "/api/ai/predict";
        log.info("Calling AI service: {} for aidRequest#{}", endpoint, aidRequestId);

        EligibilityResponseDTO response;
        try {
            response = restTemplate.postForObject(endpoint, aiRequest, EligibilityResponseDTO.class);
        } catch (Exception e) {
            log.error("AI service call failed: {}", e.getMessage());
            throw new RuntimeException("AI service unavailable: " + e.getMessage());
        }

        // ── 7. Enrichir la réponse ────────────────────────────────────────────
        if (response != null) {
            response.setAidRequestId(aidRequestId);
            response.setPatientName(aidRequest.getPatient().getFullName());
        }

        return response;
    }

    // ── Modèle médicament OCR ─────────────────────────────────────────────────

    private EligibilityResponseDTO checkEligibilityByPrescription(AidRequest aidRequest) {
        String base64 = aidRequest.getDocumentFile();

        // Aucun document uploadé → rejet immédiat sans appel IA
        if (base64 == null || base64.isBlank()) {
            log.warn("AidRequest#{} type=MEDICAMENT sans document de prescription — rejet automatique", aidRequest.getId());
            return EligibilityResponseDTO.builder()
                    .aidRequestId(aidRequest.getId())
                    .patientName(aidRequest.getPatient().getFullName())
                    .eligible(false)
                    .probability(0.0)
                    .decision("NOT_ELIGIBLE")
                    .confidence("HIGH")
                    .details(java.util.Map.of("rejection_reason",
                            "Aucun document de prescription fourni. Veuillez uploader une ordonnance."))
                    .build();
        }

        // Supprimer le préfixe data URL si présent (ex: "data:image/png;base64,...")
        if (base64.contains(",")) {
            base64 = base64.substring(base64.indexOf(',') + 1);
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(base64);
        } catch (Exception e) {
            log.error("Impossible de décoder le document base64 pour AidRequest#{}: {}", aidRequest.getId(), e.getMessage());
            throw new RuntimeException("Document de prescription invalide (base64 corrompu)");
        }

        // Construire la requête multipart
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
            @Override public String getFilename() { return "prescription.jpg"; }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", imageResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String endpoint = aiServiceUrl + "/api/ai/predict-medicament";
        log.info("Calling AI medicament model: {} for aidRequest#{}", endpoint, aidRequest.getId());

        EligibilityResponseDTO response;
        try {
            response = restTemplate.postForObject(endpoint, requestEntity, EligibilityResponseDTO.class);
        } catch (Exception e) {
            log.error("AI medicament service call failed: {}", e.getMessage());
            throw new RuntimeException("Service IA médicament indisponible: " + e.getMessage());
        }

        if (response != null) {
            response.setAidRequestId(aidRequest.getId());
            response.setPatientName(aidRequest.getPatient().getFullName());
            enrichWithDonationAvailability(response);
        }
        return response;
    }

    // ── Donation availability check ───────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void enrichWithDonationAvailability(EligibilityResponseDTO response) {
        if (response.getDetails() == null) return;
        Object medicinesObj = response.getDetails().get("medicines_detected");
        if (!(medicinesObj instanceof List<?> medicines) || medicines.isEmpty()) return;

        List<Donation> stock = donationRepository.findAvailableMedicamentDonations();
        Map<String, Boolean> availability = new LinkedHashMap<>();

        for (Object medObj : medicines) {
            if (!(medObj instanceof Map<?, ?> med)) continue;
            String name = med.get("name") != null ? med.get("name").toString() : null;
            if (name == null || name.isBlank()) continue;

            String nameLower = name.toLowerCase();
            boolean found = stock.stream().anyMatch(d -> {
                String cat  = d.getCategorie();
                String desc = d.getDescription();
                return (cat  != null && cat.toLowerCase().contains(nameLower))
                    || (desc != null && desc.toLowerCase().contains(nameLower));
            });
            availability.put(name, found);
        }
        response.setMedicineAvailability(availability);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String mapChronicDisease(String raw) {
        if (raw == null) return "NONE";
        String up = raw.toUpperCase();
        if (up.contains("DIABET"))           return "DIABETES";
        if (up.contains("HYPERT"))           return "HYPERTENSION";
        if (up.contains("HEART") || up.contains("CARDIAC")) return "HEART_DISEASE";
        if (up.contains("KIDNEY") || up.contains("RENAL")) return "KIDNEY_FAILURE";
        if (up.contains("CANCER") || up.contains("TUMOR")) return "CANCER";
        if (up.contains("MULTIPLE") || up.contains(","))   return "MULTIPLE";
        return "NONE";
    }

    private String mapDiagnosisType(String chronic) {
        return switch (chronic) {
            case "CANCER", "KIDNEY_FAILURE" -> "TERMINAL";
            case "DIABETES", "HYPERTENSION", "HEART_DISEASE", "MULTIPLE" -> "CHRONIC";
            default -> "NONE";
        };
    }

    private double estimateScorePrecarite(String chronic, int hereditary,
                                          int drugAllergy, int descLength) {
        double score = 30.0;
        score += switch (chronic) {
            case "CANCER", "KIDNEY_FAILURE" -> 35;
            case "HEART_DISEASE", "MULTIPLE" -> 25;
            case "DIABETES", "HYPERTENSION" -> 15;
            default -> 0;
        };
        score += hereditary * 10;
        score += drugAllergy * 5;
        score += Math.min(descLength / 50.0, 10);
        return Math.min(score, 100.0);
    }

    /** Retourne toutes les prédictions pour une liste de demandes d'aide */
    public List<EligibilityResponseDTO> checkAll(List<Long> aidRequestIds) {
        return aidRequestIds.stream()
                .map(id -> {
                    try { return checkEligibility(id); }
                    catch (Exception e) {
                        log.warn("Eligibility check failed for #{}: {}", id, e.getMessage());
                        return EligibilityResponseDTO.builder()
                                .aidRequestId(id)
                                .eligible(false)
                                .probability(0)
                                .decision("ERROR")
                                .confidence("LOW")
                                .build();
                    }
                })
                .toList();
    }
}
