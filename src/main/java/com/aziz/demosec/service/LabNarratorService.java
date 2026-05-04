package com.aziz.demosec.service;

import com.aziz.demosec.Entities.LabResult;
import com.aziz.demosec.Entities.Patient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@Slf4j
public class LabNarratorService {

    @Value("${groq.api-key}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private String callGpt(String systemMsg, String userMsg) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", 0.7);
            body.put("max_tokens", 500);

            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemMsg);
            messages.addObject().put("role", "user").put("content", userMsg);

            HttpEntity<String> request = new HttpEntity<>(mapper.writeValueAsString(body), headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(GROQ_URL, request, JsonNode.class);

            String content = response.getBody()
                    .get("choices").get(0)
                    .get("message").get("content").asText();

            log.info("   ✅ Groq response received ({} chars)", content.length());
            return content;

        } catch (HttpClientErrorException e) {
            log.error("   ❌ Groq HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("   ❌ Groq call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    // ── Patient version — simple language ─────────────────────────
    public String generatePatientNarrative(LabResult result, Patient patient,
                                           List<LabResult> history, Long urgentCount) {
        int age = patient.getBirthDate() != null
                ? Period.between(patient.getBirthDate(), LocalDate.now()).getYears() : 0;

        String system = """
                You are a compassionate medical assistant writing to a patient with no medical background.
                Write a warm, simple, reassuring general conclusion. Maximum 5 sentences. No jargon. English only.
                """;

        String user = """
                Write a simple general conclusion for this patient based on these 3 elements:

                1. BRAIN SCAN RESULT (VGG16 AI model):
                   - Classification: %s
                   - Risk level: %s
                   - Confidence: %.1f%%

                2. PATIENT MEDICAL PROFILE:
                   - Name: %s | Age: %d | Gender: %s
                   - Blood type: %s | Weight: %s kg | Height: %s cm
                   - Known conditions: %s
                   - Allergies: %s
                   - Glucose rate: %s

                3. HISTORY: %d previous analyses, %d were URGENT

                Start with "Dear %s," — use simple words, be warm and reassuring.
                Give one clear general conclusion about their health status.
                """.formatted(
                result.getAiDiagnostic() != null ? result.getAiDiagnostic() : "Unknown",
                result.getAiRisk() != null ? result.getAiRisk() : "Unknown",
                result.getAiConfidence() != null ? result.getAiConfidence() : 0.0,
                patient.getFullName(), age,
                patient.getGender() != null ? patient.getGender().toString() : "N/A",
                patient.getBloodType() != null ? patient.getBloodType().toString() : "N/A",
                patient.getWeight() != null ? patient.getWeight() : "N/A",
                patient.getHeight() != null ? patient.getHeight() : "N/A",
                patient.getDiseases() != null ? patient.getDiseases() : "None",
                patient.getAllergies() != null ? patient.getAllergies() : "None",
                patient.getGlucoseRate() != null ? patient.getGlucoseRate() : "N/A",
                history.size(), urgentCount != null ? urgentCount : 0,
                patient.getFullName()
        );

        String response = callGpt(system, user);
        return response != null ? response
                : "Dear " + patient.getFullName() + ", your lab analysis has been completed. Please consult your doctor for detailed information.";
    }

    // ── Doctor version — clinical language ────────────────────────
    public String generateDoctorNarrative(LabResult result, Patient patient, List<LabResult> history) {
        int age = patient.getBirthDate() != null
                ? Period.between(patient.getBirthDate(), LocalDate.now()).getYears() : 0;

        StringBuilder historyText = new StringBuilder();
        for (LabResult h : history) {
            historyText.append("- ").append(h.getCompletedAt().toLocalDate())
                    .append(": ").append(h.getAiRisk())
                    .append(" (").append(h.getAiDiagnostic()).append(")\n");
        }

        String system = """
                You are a clinical AI assistant generating reports for neurologists.
                Write a concise professional general conclusion. Maximum 6 sentences. English only.
                """;

        String user = """
                Generate a clinical general conclusion for the physician based on these 3 elements:

                1. AI MODEL RESULT (VGG16 Brain Scan):
                   - Classification: %s | Risk: %s | Confidence: %.1f%%
                   - Analysis date: %s | Abnormal findings: %s

                2. PATIENT CLINICAL PROFILE:
                   - Patient: %s | Age: %d | Gender: %s
                   - Weight: %s kg | Height: %s cm | Blood type: %s
                   - Glucose rate: %s | Known conditions: %s | Allergies: %s

                3. LONGITUDINAL HISTORY (%d analyses):
                %s

                Provide one structured clinical conclusion covering:
                - Overall assessment combining the 3 elements above
                - Risk evolution trend
                - Recommended clinical action
                Use precise neurological terminology.
                """.formatted(
                result.getAiDiagnostic() != null ? result.getAiDiagnostic() : "Unknown",
                result.getAiRisk() != null ? result.getAiRisk() : "Unknown",
                result.getAiConfidence() != null ? result.getAiConfidence() : 0.0,
                result.getCompletedAt().toLocalDate(),
                result.getAbnormalFindings() != null ? result.getAbnormalFindings() : "None reported",
                patient.getFullName(), age,
                patient.getGender() != null ? patient.getGender().toString() : "N/A",
                patient.getWeight() != null ? patient.getWeight() : "N/A",
                patient.getHeight() != null ? patient.getHeight() : "N/A",
                patient.getBloodType() != null ? patient.getBloodType().toString() : "N/A",
                patient.getGlucoseRate() != null ? patient.getGlucoseRate() : "N/A",
                patient.getDiseases() != null ? patient.getDiseases() : "None",
                patient.getAllergies() != null ? patient.getAllergies() : "None",
                history.size(),
                historyText.length() > 0 ? historyText.toString() : "No previous analyses on record."
        );

        String response = callGpt(system, user);
        return response != null ? response
                : "Clinical narrative generation failed. Please review raw result data directly in the system.";
    }
}
