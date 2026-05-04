package com.aziz.demosec.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecommendationItemDto {

    private String feature;

    @JsonAlias("current_value")
    private Double currentValue;

    @JsonAlias("healthy_baseline")
    private Double healthyBaseline;

    private String recommendation;
    private String category;
    private String priority;

    @JsonAlias("contribution_score")
    private Double contributionScore;
}
