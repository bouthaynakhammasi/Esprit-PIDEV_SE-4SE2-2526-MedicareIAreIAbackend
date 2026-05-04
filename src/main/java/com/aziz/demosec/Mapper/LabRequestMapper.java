package com.aziz.demosec.Mapper;

import com.aziz.demosec.Entities.LabRequest;
import com.aziz.demosec.dto.LabRequestRequest;
import com.aziz.demosec.dto.LabRequestResponse;
import org.springframework.stereotype.Component;

@Component
public class LabRequestMapper {

    public LabRequest toEntity(LabRequestRequest dto) {
        return LabRequest.builder()
                .requestedBy(dto.getRequestedBy())
                .testType(dto.getTestType())
                .clinicalNotes(dto.getClinicalNotes())
                .scheduledAt(dto.getScheduledAt())
                .doctorEmail(dto.getDoctorEmail())
                .build();
    }

    public LabRequestResponse toDto(LabRequest labRequest) {
        return LabRequestResponse.builder()
                .id(labRequest.getId())
                .patientId(labRequest.getPatient() != null ? labRequest.getPatient().getId() : null)
                .patientName(labRequest.getPatient() != null ? labRequest.getPatient().getFullName() : null)
                .doctorId(labRequest.getDoctor() != null ? labRequest.getDoctor().getId() : null)
                .doctorName(labRequest.getDoctor() != null ? labRequest.getDoctor().getFullName() : null)
                .laboratoryId(labRequest.getLaboratory() != null ? labRequest.getLaboratory().getId() : null)
                .laboratoryName(labRequest.getLaboratory() != null ? labRequest.getLaboratory().getName() : null)
                .requestedBy(labRequest.getRequestedBy())
                .status(labRequest.getStatus())
                .testType(labRequest.getTestType())
                .clinicalNotes(labRequest.getClinicalNotes())
                .doctorEmail(labRequest.getDoctorEmail())
                .scheduledAt(labRequest.getScheduledAt())
                .requestedAt(labRequest.getRequestedAt())
                .notificationSent(labRequest.isNotificationSent())
                .hasResult(labRequest.getLabResult() != null)
                .build();
    }

    public void updateFromDto(LabRequestRequest dto, LabRequest entity) {
        entity.setTestType(dto.getTestType());
        entity.setClinicalNotes(dto.getClinicalNotes());
        entity.setScheduledAt(dto.getScheduledAt());
        entity.setDoctorEmail(dto.getDoctorEmail());
        if (dto.getRequestedBy() != null) {
            entity.setRequestedBy(dto.getRequestedBy());
        }
    }
}