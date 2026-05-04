package com.aziz.demosec.controller;

import com.aziz.demosec.dto.ClinicalDataRequest;
import com.aziz.demosec.dto.ClinicalRecommendationResponse;
import com.aziz.demosec.service.ClinicalRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alzheimer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ClinicalRecommendationController {

    private final ClinicalRecommendationService clinicalRecommendationService;

    @PostMapping("/clinical-recommend/{labResultId}")
    public ResponseEntity<ClinicalRecommendationResponse> recommend(
            @PathVariable Long labResultId,
            @RequestBody ClinicalDataRequest request) {

        ClinicalRecommendationResponse result = clinicalRecommendationService.recommend(labResultId, request);
        return ResponseEntity.ok(result);
    }
}
