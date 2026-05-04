// IEmergencyService.java
package com.aziz.demosec.service;

import com.aziz.demosec.Entities.EmergencyAlertStatus;
import com.aziz.demosec.Entities.EmergencyInterventionStatus;
import com.aziz.demosec.dto.emergency.*;
import java.util.List;

public interface IEmergencyService {

    // ─── SMART DEVICE ─────────────────────────────────────────────
    SmartDeviceResponseDTO createSmartDevice(SmartDeviceRequestDTO dto);
    SmartDeviceResponseDTO getSmartDeviceById(Long id);
    SmartDeviceResponseDTO getSmartDeviceByPatientId(Long patientId);
    List<SmartDeviceResponseDTO> getAllSmartDevices();
    void deleteSmartDevice(Long id);

    // ─── EMERGENCY ALERT ──────────────────────────────────────────
    EmergencyAlertResponseDTO createAlert(EmergencyAlertRequestDTO dto);
    EmergencyAlertResponseDTO getAlertById(Long id);
    List<EmergencyAlertResponseDTO> getAllAlerts();
    List<EmergencyAlertResponseDTO> getAlertsByStatus(EmergencyAlertStatus status);
    EmergencyAlertResponseDTO updateAlertStatus(Long id, EmergencyAlertStatus status);
    EmergencyAlertResponseDTO cancelAlertByPatient(Long id);

    // ─── AMBULANCE ────────────────────────────────────────────────
    AmbulanceResponseDTO createAmbulance(AmbulanceRequestDTO dto);
    AmbulanceResponseDTO getAmbulanceById(Long id);
    List<AmbulanceResponseDTO> getAllAmbulances();
    List<AmbulanceResponseDTO> getAmbulancesByClinic(Long clinicId);
    AmbulanceResponseDTO updateAmbulance(Long id, AmbulanceRequestDTO dto);
    void deleteAmbulance(Long id);

    // ─── INTERVENTION ─────────────────────────────────────────────
    EmergencyInterventionResponseDTO dispatchIntervention(EmergencyInterventionRequestDTO dto);
    AutoDispatchResponseDTO autoDispatch(Long alertId);
    EmergencyInterventionResponseDTO getInterventionById(Long id);
    List<EmergencyInterventionResponseDTO> getAllInterventions();
    EmergencyInterventionResponseDTO updateInterventionStatus(Long id, EmergencyInterventionStatus status);
}
