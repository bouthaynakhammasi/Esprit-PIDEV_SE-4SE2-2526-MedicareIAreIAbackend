package com.aziz.demosec.service;

import com.aziz.demosec.dto.LabResultResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AlzheimerAiService {

    LabResultResponse analyzeAndSave(Long labRequestId, MultipartFile image, String technicianName, String doctorEmail);
}
