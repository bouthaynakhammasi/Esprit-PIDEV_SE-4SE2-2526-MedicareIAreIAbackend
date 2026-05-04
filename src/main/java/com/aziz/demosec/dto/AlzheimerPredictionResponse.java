package com.aziz.demosec.dto;

import lombok.*;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AlzheimerPredictionResponse {

    private String diagnostic;
    private String risque;
    private String couleur;
    private Double confiance;
    private Map<String, Double> probabilites;
    private String message;
    private String note;
}
