package com.aziz.demosec.dto.homecare;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderScoreDTO {
    private Long providerId;
    private String providerName;
    private double rating;
    private int workload;
    private boolean available;
    private double score;
}
