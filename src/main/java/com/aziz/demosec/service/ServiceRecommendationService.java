package com.aziz.demosec.service;

import com.aziz.demosec.dto.homecare.ServiceRecommendationDTO;
import com.aziz.demosec.dto.homecare.SymptomCheckRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRecommendationService {

    private static final String ML_API_URL = "http://localhost:8000/recommend-service";

    private final RestTemplate restTemplate;

    public ServiceRecommendationDTO recommend(SymptomCheckRequestDTO request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of("symptoms", request.getSymptoms());
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<ServiceRecommendationDTO> response = restTemplate.exchange(
                    ML_API_URL,
                    HttpMethod.POST,
                    entity,
                    ServiceRecommendationDTO.class
            );

            log.info("ML recommendation for symptoms '{}': {}",
                    request.getSymptoms().substring(0, Math.min(50, request.getSymptoms().length())),
                    response.getBody() != null ? response.getBody().getRecommendedService() : "null");

            return response.getBody();

        } catch (Exception e) {
            log.error("ML service unavailable: {}", e.getMessage());
            throw new RuntimeException("Service de recommandation temporairement indisponible. Veuillez réessayer.");
        }
    }
}
