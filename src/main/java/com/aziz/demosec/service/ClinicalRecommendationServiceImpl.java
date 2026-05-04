package com.aziz.demosec.service;

import com.aziz.demosec.Entities.LabRequest;
import com.aziz.demosec.Entities.LabResult;
import com.aziz.demosec.dto.ClinicalDataRequest;
import com.aziz.demosec.dto.ClinicalRecommendationResponse;
import com.aziz.demosec.exception.ResourceNotFoundException;
import com.aziz.demosec.repository.LabResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicalRecommendationServiceImpl implements ClinicalRecommendationService {

    private final LabResultRepository labResultRepository;
    private final RestTemplate restTemplate;
    private final EmailService emailService;

    @Value("${recommendation.api.url}")
    private String recommendApiUrl;

    @Override
    public ClinicalRecommendationResponse recommend(Long labResultId, ClinicalDataRequest request) {

        LabResult labResult = labResultRepository.findById(labResultId)
                .orElseThrow(() -> new ResourceNotFoundException("LabResult not found: " + labResultId));

        LabRequest labRequest = labResult.getLabRequest();

        Map<String, Object> fastApiBody = buildFastApiRequest(request, labRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(fastApiBody, headers);

        ResponseEntity<ClinicalRecommendationResponse> response = restTemplate.exchange(
                recommendApiUrl + "/recommend",
                HttpMethod.POST,
                entity,
                ClinicalRecommendationResponse.class
        );

        ClinicalRecommendationResponse result = response.getBody();
        if (result == null) {
            throw new RuntimeException("Empty response from Recommendation AI service");
        }

        String targetEmail = resolveEmail(request, labRequest);

        if (targetEmail != null) {
            try {
                String patientName = labRequest.getPatient() != null
                        ? labRequest.getPatient().getFullName() : "Patient inconnu";

                emailService.sendCombinedAlzheimerReport(
                        targetEmail,
                        patientName,
                        labResult.getAiDiagnostic(),
                        labResult.getAiRisk(),
                        labResult.getAiConfidence(),
                        result.getRiskScore(),
                        result.getRiskLabel(),
                        result.getRiskMessage(),
                        result.getRecommendations()
                );
                result.setEmailSent(true);
            } catch (Exception e) {
                log.error("Combined report email failed for labResult {}: {}", labResultId, e.getMessage());
            }
        }

        return result;
    }

    private String resolveEmail(ClinicalDataRequest req, LabRequest labRequest) {
        if (req.getDoctorEmail() != null && !req.getDoctorEmail().isBlank()) {
            return req.getDoctorEmail();
        }
        if (labRequest.getDoctorEmail() != null && !labRequest.getDoctorEmail().isBlank()) {
            return labRequest.getDoctorEmail();
        }
        if (labRequest.getDoctor() != null) {
            return labRequest.getDoctor().getEmail();
        }
        return null;
    }

    private Map<String, Object> buildFastApiRequest(ClinicalDataRequest req, LabRequest labRequest) {
        String patientName = labRequest.getPatient() != null
                ? labRequest.getPatient().getFullName() : "Patient";
        String doctorName = labRequest.getDoctor() != null
                ? labRequest.getDoctor().getFullName() : "Médecin";

        Map<String, Object> body = new HashMap<>();
        body.put("nom", patientName);
        body.put("age_patient", req.getAge() != null ? req.getAge().intValue() : 0);
        body.put("medecin", doctorName);
        body.put("date", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        body.put("Age",                       req.getAge());
        body.put("Gender",                    req.getGender());
        body.put("Ethnicity",                 req.getEthnicity());
        body.put("EducationLevel",            req.getEducationLevel());
        body.put("BMI",                       req.getBmi());
        body.put("Smoking",                   req.getSmoking());
        body.put("AlcoholConsumption",        req.getAlcoholConsumption());
        body.put("PhysicalActivity",          req.getPhysicalActivity());
        body.put("DietQuality",               req.getDietQuality());
        body.put("SleepQuality",              req.getSleepQuality());
        body.put("FamilyHistoryAlzheimers",   req.getFamilyHistoryAlzheimers());
        body.put("CardiovascularDisease",     req.getCardiovascularDisease());
        body.put("Diabetes",                  req.getDiabetes());
        body.put("Depression",                req.getDepression());
        body.put("HeadInjury",                req.getHeadInjury());
        body.put("Hypertension",              req.getHypertension());
        body.put("SystolicBP",                req.getSystolicBP());
        body.put("DiastolicBP",               req.getDiastolicBP());
        body.put("CholesterolTotal",          req.getCholesterolTotal());
        body.put("CholesterolLDL",            req.getCholesterolLDL());
        body.put("CholesterolHDL",            req.getCholesterolHDL());
        body.put("CholesterolTriglycerides",  req.getCholesterolTriglycerides());
        body.put("MMSE",                      req.getMmse());
        body.put("FunctionalAssessment",      req.getFunctionalAssessment());
        body.put("MemoryComplaints",          req.getMemoryComplaints());
        body.put("BehavioralProblems",        req.getBehavioralProblems());
        body.put("ADL",                       req.getAdl());
        body.put("Confusion",                 req.getConfusion());
        body.put("Disorientation",            req.getDisorientation());
        body.put("PersonalityChanges",        req.getPersonalityChanges());
        body.put("DifficultyCompletingTasks", req.getDifficultyCompletingTasks());
        body.put("Forgetfulness",             req.getForgetfulness());

        return body;
    }
}
