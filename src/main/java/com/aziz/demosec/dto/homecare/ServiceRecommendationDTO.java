package com.aziz.demosec.dto.homecare;

import lombok.Data;
import java.util.List;

@Data
public class ServiceRecommendationDTO {

    private String recommendedService;
    private double confidence;
    private String urgencyLevel;
    private List<String> detectedKeywords;
    private List<AlternativeServiceDTO> alternatives;
    private String message;

    @Data
    public static class AlternativeServiceDTO {
        private String service;
        private double confidence;
    }
}
