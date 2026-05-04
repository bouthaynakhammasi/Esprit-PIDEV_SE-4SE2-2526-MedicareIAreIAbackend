package com.aziz.demosec.service;

import com.aziz.demosec.Entities.LabRequestStatus;
import com.aziz.demosec.dto.LabRequestRequest;
import com.aziz.demosec.dto.LabRequestResponse;
import com.aziz.demosec.dto.RequestedBy;

import java.util.List;

public interface LabRequestService {

    LabRequestResponse create(LabRequestRequest request);
    LabRequestResponse getById(Long id);
    List<LabRequestResponse> getAll();
    LabRequestResponse update(Long id, LabRequestRequest request);
    void delete(Long id);

    List<LabRequestResponse> getByPatient(Long patientId);
    List<LabRequestResponse> getByDoctor(Long doctorId);
    List<LabRequestResponse> getByLaboratory(Long laboratoryId);
    List<LabRequestResponse> getByStatus(LabRequestStatus status);
    List<LabRequestResponse> getByRequestedBy(RequestedBy requestedBy);
    List<LabRequestResponse> getPatientHistory(Long patientId);

    LabRequestResponse updateStatus(Long id, LabRequestStatus status);
    LabRequestResponse cancel(Long id);
    LabRequestResponse markNotificationSent(Long id);
    List<LabRequestResponse> getPendingByLaboratory(Long laboratoryId);
    List<LabRequestResponse> getByLaboratoryAndStatusWithDetails(
            Long laboratoryId, LabRequestStatus status);

    List<LabRequestResponse> searchByPatientLastName(
            String lastName, LabRequestStatus status);

    List<LabRequestResponse> searchByLaboratoryName(
            String labName, LabRequestStatus status, String testType);
}