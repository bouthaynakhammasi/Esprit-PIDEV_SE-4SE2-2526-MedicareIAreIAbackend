package com.aziz.demosec.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClinicalRecommendationResponse {

    private Integer diagnosis;

    @JsonAlias("risk_score")
    private Double riskScore;

    @JsonAlias("risk_label")
    private String riskLabel;

    @JsonAlias("risk_color")
    private String riskColor;

    @JsonAlias("risk_message")
    private String riskMessage;

    @JsonAlias("prob_rf")
    private Double probRf;

    @JsonAlias("total_recommendations")
    private Integer totalRecommendations;

    private List<RecommendationItemDto> recommendations;

    private boolean emailSent;
}
