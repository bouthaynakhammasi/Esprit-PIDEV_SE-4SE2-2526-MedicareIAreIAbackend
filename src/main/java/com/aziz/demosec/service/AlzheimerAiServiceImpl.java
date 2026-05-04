package com.aziz.demosec.service;

import com.aziz.demosec.Entities.LabRequest;
import com.aziz.demosec.Entities.LabRequestStatus;
import com.aziz.demosec.Entities.LabResult;
import com.aziz.demosec.Mapper.LabResultMapper;
import com.aziz.demosec.dto.AlzheimerPredictionResponse;
import com.aziz.demosec.dto.LabResultResponse;
import com.aziz.demosec.exception.ResourceNotFoundException;
import com.aziz.demosec.repository.LabRequestRepository;
import com.aziz.demosec.repository.LabResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlzheimerAiServiceImpl implements AlzheimerAiService {

    private final LabRequestRepository labRequestRepository;
    private final LabResultRepository labResultRepository;
    private final LabResultMapper labResultMapper;
    private final RestTemplate restTemplate;
    private final EmailService emailService;

    @Value("${alzheimer.api.url}")
    private String alzheimerApiUrl;

    @Override
    public LabResultResponse analyzeAndSave(Long labRequestId, MultipartFile image, String technicianName, String doctorEmail) {

        if (labResultRepository.existsByLabRequestId(labRequestId)) {
            throw new IllegalStateException("A result already exists for this lab request.");
        }

        LabRequest labRequest = labRequestRepository.findById(labRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LabRequest not found: " + labRequestId));

        AlzheimerPredictionResponse prediction = callFastApi(image, labRequest);

        boolean isAbnormal = !"SAIN".equalsIgnoreCase(prediction.getRisque());

        LabResult result = LabResult.builder()
                .labRequest(labRequest)
                .technicianName(technicianName)
                .resultData(buildResultData(prediction))
                .isAbnormal(isAbnormal)
                .abnormalFindings(isAbnormal ? prediction.getMessage() : null)
                .status("COMPLETED")
                .completedAt(LocalDateTime.now())
                .aiDiagnostic(prediction.getDiagnostic())
                .aiRisk(prediction.getRisque())
                .aiConfidence(prediction.getConfiance())
                .aiAlertSent(false)
                .build();

        labRequest.setStatus(LabRequestStatus.COMPLETED);
        labRequestRepository.save(labRequest);
        LabResult saved = labResultRepository.save(result);

        String targetEmail = (doctorEmail != null && !doctorEmail.isBlank())
                ? doctorEmail
                : (labRequest.getDoctor() != null ? labRequest.getDoctor().getEmail() : null);

        if (targetEmail != null) {
            try {
                String patientName = labRequest.getPatient() != null
                        ? labRequest.getPatient().getFullName() : "Patient inconnu";

                emailService.sendAlzheimerReport(
                        targetEmail,
                        targetEmail,
                        patientName,
                        technicianName,
                        prediction.getDiagnostic(),
                        prediction.getRisque(),
                        prediction.getConfiance(),
                        prediction.getMessage(),
                        buildResultData(prediction),
                        prediction.getProbabilites()
                );

                saved.setAiAlertSent(true);
                saved = labResultRepository.save(saved);
            } catch (Exception e) {
                log.error("Alzheimer report email failed for labRequest {}: {}", labRequestId, e.getMessage());
            }
        }

        return labResultMapper.toResponse(saved);
    }

    private AlzheimerPredictionResponse callFastApi(MultipartFile image, LabRequest labRequest) {
        try {
            String patientName = labRequest.getPatient() != null
                    ? labRequest.getPatient().getFullName() : "Patient";
            String doctorName  = labRequest.getDoctor() != null
                    ? labRequest.getDoctor().getFullName() : "Médecin";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ByteArrayResource imageResource = new ByteArrayResource(image.getBytes()) {
                @Override
                public String getFilename() {
                    return image.getOriginalFilename() != null ? image.getOriginalFilename() : "mri.jpg";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", imageResource);
            body.add("nom", patientName);
            body.add("age", 0);
            body.add("medecin", doctorName);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<AlzheimerPredictionResponse> response = restTemplate.exchange(
                    alzheimerApiUrl + "/predict/patient",
                    HttpMethod.POST,
                    request,
                    AlzheimerPredictionResponse.class
            );

            if (response.getBody() == null) {
                throw new RuntimeException("Empty response from Alzheimer AI service");
            }
            return response.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Failed to call Alzheimer AI service: " + e.getMessage(), e);
        }
    }

    private String buildResultData(AlzheimerPredictionResponse p) {
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnostic: ").append(p.getDiagnostic())
          .append(" | Risque: ").append(p.getRisque())
          .append(" | Confiance: ").append(p.getConfiance()).append("%");

        if (p.getProbabilites() != null) {
            sb.append(" | Probabilités: ");
            p.getProbabilites().forEach((k, v) -> sb.append(k).append("=").append(v).append("% "));
        }
        return sb.toString().trim();
    }
}
