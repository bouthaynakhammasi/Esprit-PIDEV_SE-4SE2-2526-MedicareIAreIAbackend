package com.aziz.demosec.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliveryPredictionService {

    private final RestTemplate restTemplate;

    @Value("${ml.fastapi.url}")
    private String fastApiUrl;

    public int predict(double distanceKm, String city, int nbItems) {
        try {
            int hour = LocalDateTime.now().getHour();
            int prepTime = computePreparationTime(nbItems);

            Map<String, Object> body = new HashMap<>();
            body.put("distance_km", distanceKm);
            body.put("city", city);
            body.put("preparation_time_min", prepTime);
            body.put("hour", hour);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                fastApiUrl + "/predict-delivery", body, Map.class
            );

            if (response != null && response.containsKey("estimated_delivery_min")) {
                return ((Number) response.get("estimated_delivery_min")).intValue();
            }
        } catch (Exception e) {
            System.err.println("ML prediction failed: " + e.getMessage() + " — fallback 45min");
        }
        return 45;
    }

    private int computePreparationTime(int nbItems) {
        if (nbItems <= 3) return 10;
        if (nbItems <= 7) return 15;
        return 20;
    }
}
