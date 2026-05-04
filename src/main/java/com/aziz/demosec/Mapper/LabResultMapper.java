package com.aziz.demosec.Mapper;

import com.aziz.demosec.Entities.LabResult;
import com.aziz.demosec.Entities.LabRequest;
import com.aziz.demosec.dto.LabResultRequest;
import com.aziz.demosec.dto.LabResultResponse;
import org.springframework.stereotype.Component;

@Component
public class LabResultMapper {

    public LabResult toEntity(LabResultRequest dto, LabRequest labRequest) {
        if (dto == null) return null;
        
        return LabResult.builder()
                .labRequest(labRequest)
                .resultData(dto.getResultData())
                .technicianName(dto.getTechnicianName())
                .isAbnormal(dto.getIsAbnormal())
                .status(dto.getStatus())
                .abnormalFindings(dto.getAbnormalFindings())
                .verifiedBy(dto.getVerifiedBy())
                .build();
    }

    public LabResultResponse toResponse(LabResult labResult) {
        if (labResult == null) return null;

        String testType = null;
        String patientName = null;
        String laboratoryName = null;
        Long labRequestId = null;

        if (labResult.getLabRequest() != null) {
            labRequestId = labResult.getLabRequest().getId();
            testType = labResult.getLabRequest().getTestType();
            if (labResult.getLabRequest().getPatient() != null) {
                patientName = labResult.getLabRequest().getPatient().getFullName();
            }
            if (labResult.getLabRequest().getLaboratory() != null) {
                laboratoryName = labResult.getLabRequest().getLaboratory().getName();
            }
        }

        return LabResultResponse.builder()
                .id(labResult.getId())
                .labRequestId(labRequestId)
                .testType(testType)
                .patientName(patientName)
                .laboratoryName(laboratoryName)
                .resultData(labResult.getResultData())
                .technicianName(labResult.getTechnicianName())
                .verifiedBy(labResult.getVerifiedBy())
                .abnormalFindings(labResult.getAbnormalFindings())
                .status(labResult.getStatus())
                .isAbnormal(labResult.getIsAbnormal())
                .completedAt(labResult.getCompletedAt())
                .verifiedAt(labResult.getVerifiedAt())
                .aiDiagnostic(labResult.getAiDiagnostic())
                .aiRisk(labResult.getAiRisk())
                .aiConfidence(labResult.getAiConfidence())
                .aiAlertSent(labResult.getAiAlertSent())
                .build();
    }

    public void updateEntityFromRequest(LabResultRequest dto, LabResult labResult) {
        if (dto == null || labResult == null) return;
        
        if (dto.getResultData() != null) {
            labResult.setResultData(dto.getResultData());
        }
        if (dto.getTechnicianName() != null) {
            labResult.setTechnicianName(dto.getTechnicianName());
        }
        if (dto.getIsAbnormal() != null) {
            labResult.setIsAbnormal(dto.getIsAbnormal());
        }
        if (dto.getStatus() != null) {
            labResult.setStatus(dto.getStatus());
        }
        if (dto.getAbnormalFindings() != null) {
            labResult.setAbnormalFindings(dto.getAbnormalFindings());
        }
        if (dto.getVerifiedBy() != null) {
            labResult.setVerifiedBy(dto.getVerifiedBy());
        }
    }
}