package com.aziz.demosec.service;

import com.aziz.demosec.dto.ClinicalDataRequest;
import com.aziz.demosec.dto.ClinicalRecommendationResponse;

public interface ClinicalRecommendationService {
    ClinicalRecommendationResponse recommend(Long labResultId, ClinicalDataRequest request);
}
