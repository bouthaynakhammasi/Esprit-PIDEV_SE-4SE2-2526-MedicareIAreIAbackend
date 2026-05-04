package com.aziz.demosec.controller;

import com.aziz.demosec.dto.LabResultResponse;
import com.aziz.demosec.service.AlzheimerAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/alzheimer")
@RequiredArgsConstructor
public class AlzheimerAnalysisController {

    private final AlzheimerAiService alzheimerAiService;

    /**
     * POST /api/alzheimer/analyze/{labRequestId}
     * Upload MRI image → calls FastAPI → saves LabResult → alerts doctor if needed
     */
    @PostMapping(value = "/analyze/{labRequestId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LabResultResponse> analyze(
            @PathVariable Long labRequestId,
            @RequestParam("image") MultipartFile image,
            @RequestParam("technicianName") String technicianName,
            @RequestParam(value = "doctorEmail", required = false, defaultValue = "") String doctorEmail) {

        LabResultResponse response = alzheimerAiService.analyzeAndSave(labRequestId, image, technicianName, doctorEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
