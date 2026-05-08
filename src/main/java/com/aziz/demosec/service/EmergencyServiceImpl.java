// EmergencyService.java
package com.aziz.demosec.service;

import com.aziz.demosec.Entities.*;
import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.emergency.*;
import com.aziz.demosec.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyServiceImpl implements IEmergencyService {

    private final SmartDeviceRepository smartDeviceRepository;
    private final EmergencyAlertRepository alertRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final EmergencyInterventionRepository interventionRepository;
    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final WsNotificationService wsNotificationService;

    // ─── SMART DEVICE ─────────────────────────────────────────────

    @Override
    public SmartDeviceResponseDTO createSmartDevice(SmartDeviceRequestDTO dto) {
        if (smartDeviceRepository.existsByPatientId(dto.getPatientId())) {
            throw new RuntimeException("Patient already has a SmartDevice");
        }
        User patient = userRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        SmartDevice device = SmartDevice.builder()
                .patient(patient)
                .build();

        return toSmartDeviceDTO(smartDeviceRepository.save(device));
    }

    @Override
    public SmartDeviceResponseDTO getSmartDeviceById(Long id) {
        return toSmartDeviceDTO(findDeviceById(id));
    }

    @Override
    public List<SmartDeviceResponseDTO> getAllSmartDevices() {
        return smartDeviceRepository.findAll()
                .stream().map(this::toSmartDeviceDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SmartDeviceResponseDTO getSmartDeviceByPatientId(Long patientId) {
        SmartDevice device = smartDeviceRepository.findByPatientId(patientId)
                .orElseThrow(() -> new RuntimeException("No SmartDevice found for patient: " + patientId));
        return toSmartDeviceDTO(device);
    }

    @Override
    public void deleteSmartDevice(Long id) {
        smartDeviceRepository.deleteById(id);
    }

    // ─── EMERGENCY ALERT ──────────────────────────────────────────

    @Override
    public EmergencyAlertResponseDTO createAlert(EmergencyAlertRequestDTO dto) {
        SmartDevice device = findDeviceById(dto.getSmartDeviceId());

        EmergencyAlert alert = EmergencyAlert.builder()
                .device(device)
                .severity(dto.getSeverity())
                .status(EmergencyAlertStatus.PENDING)
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .canceledByPatient(false)
                .build();

        EmergencyAlert saved = alertRepository.save(alert);

        // 🔔 Notifier toutes les cliniques via WebSocket
        // 2 JOINs : EmergencyAlert → SmartDevice → Patient
        String patientName = alertRepository.findPatientByAlertId(saved.getId())
                .map(u -> u.getFullName())
                .orElse("Un patient");
        wsNotificationService.notifyClinic(
                "🚨 Nouvelle alerte d'urgence",
                patientName + " a envoyé une demande d'urgence.",
                "warning"
        );

        return toAlertDTO(saved);
    }

    @Override
    public EmergencyAlertResponseDTO getAlertById(Long id) {
        return toAlertDTO(findAlertById(id));
    }

    @Override
    public List<EmergencyAlertResponseDTO> getAllAlerts() {
        return alertRepository.findAll()
                .stream().map(this::toAlertDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmergencyAlertResponseDTO> getAlertsByStatus(EmergencyAlertStatus status) {
        return alertRepository.findByStatus(status)
                .stream().map(this::toAlertDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EmergencyAlertResponseDTO updateAlertStatus(Long id, EmergencyAlertStatus status) {
        EmergencyAlert alert = findAlertById(id);
        alert.setStatus(status);
        return toAlertDTO(alertRepository.save(alert));
    }

    @Override
    public EmergencyAlertResponseDTO cancelAlertByPatient(Long id) {
        EmergencyAlert alert = findAlertById(id);
        alert.setCanceledByPatient(true);
        alert.setStatus(EmergencyAlertStatus.RESOLVED);
        return toAlertDTO(alertRepository.save(alert));
    }

    // ─── AMBULANCE ────────────────────────────────────────────────

    @Override
    public AmbulanceResponseDTO createAmbulance(AmbulanceRequestDTO dto) {
        // Cherche d'abord par userId (= ID de l'utilisateur connecté), puis par Clinic.id
        Clinic clinic = clinicRepository.findByUserId(dto.getClinicId())
                .orElseGet(() -> clinicRepository.findById(dto.getClinicId())
                        .orElseThrow(() -> new RuntimeException("Clinic not found for userId=" + dto.getClinicId())));

        Ambulance ambulance = Ambulance.builder()
                .clinic(clinic)
                .currentLat(dto.getCurrentLat())
                .currentLng(dto.getCurrentLng())
                .licensePlate(dto.getLicensePlate())
                .status(dto.getStatus() != null ? dto.getStatus() : "AVAILABLE")
                .build();

        return toAmbulanceDTO(ambulanceRepository.save(ambulance));
    }

    @Override
    public AmbulanceResponseDTO getAmbulanceById(Long id) {
        return toAmbulanceDTO(findAmbulanceById(id));
    }

    @Override
    public List<AmbulanceResponseDTO> getAllAmbulances() {
        return ambulanceRepository.findAll()
                .stream().map(this::toAmbulanceDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AmbulanceResponseDTO> getAmbulancesByClinic(Long userId) {
        // Résoudre le vrai Clinic.id depuis userId
        Long realClinicId = clinicRepository.findByUserId(userId)
                .map(c -> c.getId())
                .orElse(userId); // fallback si ancienne donnée
        return ambulanceRepository.findByClinicId(realClinicId)
                .stream().map(this::toAmbulanceDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AmbulanceResponseDTO updateAmbulance(Long id, AmbulanceRequestDTO dto) {
        Ambulance ambulance = findAmbulanceById(id);
        ambulance.setCurrentLat(dto.getCurrentLat());
        ambulance.setCurrentLng(dto.getCurrentLng());
        if (dto.getLicensePlate() != null) {
            ambulance.setLicensePlate(dto.getLicensePlate());
        }
        if (dto.getStatus() != null) {
            ambulance.setStatus(dto.getStatus());
        }
        return toAmbulanceDTO(ambulanceRepository.save(ambulance));
    }

    @Override
    public void deleteAmbulance(Long id) {
        ambulanceRepository.deleteById(id);
    }

    // ─── INTERVENTION ─────────────────────────────────────────────

    @Override
    @Transactional
    public AutoDispatchResponseDTO autoDispatch(Long alertId) {
        EmergencyAlert alert = findAlertById(alertId);

        // 1. L'alerte doit être dans un état dispatchable (PENDING ou ACKNOWLEDGED)
        if (alert.getStatus() == EmergencyAlertStatus.RESOLVED
                || alert.getStatus() == EmergencyAlertStatus.CANCELED
                || alert.getStatus() == EmergencyAlertStatus.CLINIC_NOTIFIED) {
            throw new RuntimeException(
                "Alert #" + alertId + " cannot be dispatched — current status: " + alert.getStatus());
        }

        // 2. Empêcher le double-dispatch sur la même alerte
        if (alert.getIntervention() != null) {
            throw new RuntimeException("Alert #" + alertId + " already has an intervention dispatched");
        }

        // 3. Trouver les ambulances AVAILABLE avec coordonnées GPS
        List<Ambulance> available = ambulanceRepository.findByStatus("AVAILABLE").stream()
                .filter(a -> a.getCurrentLat() != null && a.getCurrentLng() != null)
                .collect(Collectors.toList());

        if (available.isEmpty()) {
            throw new RuntimeException("No available ambulances with GPS coordinates found");
        }

        // 4. Haversine : choisir l'ambulance la plus proche
        Ambulance closest = null;
        double minDistance = Double.MAX_VALUE;
        for (Ambulance amb : available) {
            double dist = haversineDistance(
                    alert.getLatitude(), alert.getLongitude(),
                    amb.getCurrentLat(), amb.getCurrentLng());
            if (!Double.isNaN(dist) && dist < minDistance) {
                minDistance = dist;
                closest = amb;
            }
        }

        if (closest == null) {
            throw new RuntimeException("Could not calculate distance to any available ambulance");
        }

        // 5. L'ambulance doit avoir une clinique associée
        Clinic clinic = closest.getClinic();
        if (clinic == null) {
            throw new RuntimeException("Ambulance #" + closest.getId() + " has no associated clinic");
        }

        // 6. Ambulance → ON_DUTY
        closest.setStatus("ON_DUTY");
        ambulanceRepository.save(closest);

        // 7. Alerte → CLINIC_NOTIFIED (prise en charge, ni annulée ni résolue)
        alert.setStatus(EmergencyAlertStatus.CLINIC_NOTIFIED);
        alertRepository.save(alert);

        // 8. Créer l'intervention DISPATCHED
        EmergencyIntervention intervention = EmergencyIntervention.builder()
                .emergencyAlert(alert)
                .clinic(clinic)
                .ambulance(closest)
                .dispatchedAt(LocalDateTime.now())
                .status(EmergencyInterventionStatus.DISPATCHED)
                .build();
        EmergencyIntervention saved = interventionRepository.save(intervention);

        log.info("Auto-dispatch OK — alert#{} → ambulance#{} ({}) dist={}km",
                alertId, closest.getId(), closest.getLicensePlate(),
                Math.round(minDistance * 10.0) / 10.0);

        return AutoDispatchResponseDTO.builder()
                .interventionId(saved.getId())
                .emergencyAlertId(alertId)
                .patientName(alert.getDevice().getPatient().getFullName())
                .ambulanceId(closest.getId())
                .ambulanceLicensePlate(closest.getLicensePlate())
                .clinicId(clinic.getId())
                .clinicName(clinic.getName())
                .distanceKm(Math.round(minDistance * 10.0) / 10.0)
                .status(EmergencyInterventionStatus.DISPATCHED.name())
                .dispatchedAt(saved.getDispatchedAt())
                .build();
    }

    @Override
    public EmergencyInterventionResponseDTO dispatchIntervention(EmergencyInterventionRequestDTO dto) {
        EmergencyAlert alert = findAlertById(dto.getEmergencyAlertId());
        Clinic clinic = clinicRepository.findByUserId(dto.getClinicId())
                .orElseGet(() -> clinicRepository.findById(dto.getClinicId())
                        .orElseThrow(() -> new RuntimeException("Clinic not found for userId=" + dto.getClinicId())));
        Ambulance ambulance = findAmbulanceById(dto.getAmbulanceId());

        // Mettre à jour le statut de l'alerte et de l'ambulance
        alert.setStatus(EmergencyAlertStatus.CLINIC_NOTIFIED);
        alertRepository.save(alert);

        ambulance.setStatus("ON_DUTY");
        ambulanceRepository.save(ambulance);

        EmergencyIntervention intervention = EmergencyIntervention.builder()
                .emergencyAlert(alert)
                .clinic(clinic)
                .ambulance(ambulance)
                .dispatchedAt(LocalDateTime.now())
                .status(EmergencyInterventionStatus.DISPATCHED)
                .build();

        return toInterventionDTO(interventionRepository.save(intervention));
    }

    @Override
    public EmergencyInterventionResponseDTO getInterventionById(Long id) {
        return toInterventionDTO(findInterventionById(id));
    }

    @Override
    public List<EmergencyInterventionResponseDTO> getAllInterventions() {
        return interventionRepository.findAll()
                .stream().map(this::toInterventionDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EmergencyInterventionResponseDTO updateInterventionStatus(Long id, EmergencyInterventionStatus status) {
        EmergencyIntervention intervention = findInterventionById(id);
        intervention.setStatus(status);

        if (status == EmergencyInterventionStatus.ARRIVED) {
            intervention.setArrivedAt(LocalDateTime.now());
        }
        if (status == EmergencyInterventionStatus.COMPLETED) {
            intervention.setCompletedAt(LocalDateTime.now());
        }

        return toInterventionDTO(interventionRepository.save(intervention));
    }

    // ─── HELPERS PRIVÉS ───────────────────────────────────────────

    private SmartDevice findDeviceById(Long id) {
        return smartDeviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SmartDevice not found: " + id));
    }

    private EmergencyAlert findAlertById(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + id));
    }

    private Ambulance findAmbulanceById(Long id) {
        return ambulanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ambulance not found: " + id));
    }

    private EmergencyIntervention findInterventionById(Long id) {
        return interventionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Intervention not found: " + id));
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ─── MAPPERS ──────────────────────────────────────────────────

    private SmartDeviceResponseDTO toSmartDeviceDTO(SmartDevice d) {
        SmartDeviceResponseDTO.SmartDeviceResponseDTOBuilder builder = SmartDeviceResponseDTO.builder()
                .id(d.getId())
                .patientId(d.getPatient().getId())
                .patientName(d.getPatient().getFullName());
        if (d.getPatient() instanceof Patient patient) {
            builder.emergencyContactName(patient.getEmergencyContactName())
                   .emergencyContactPhone(patient.getEmergencyContactPhone());
        }
        return builder.build();
    }

    private EmergencyAlertResponseDTO toAlertDTO(EmergencyAlert a) {
        return EmergencyAlertResponseDTO.builder()
                .id(a.getId())
                .smartDeviceId(a.getDevice().getId())
                .patientName(a.getDevice().getPatient().getFullName())
                .severity(a.getSeverity())
                .status(a.getStatus())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .canceledByPatient(a.getCanceledByPatient())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private AmbulanceResponseDTO toAmbulanceDTO(Ambulance a) {
        return AmbulanceResponseDTO.builder()
                .id(a.getId())
                .clinicId(a.getClinic() != null ? a.getClinic().getId() : null)
                .currentLat(a.getCurrentLat())
                .currentLng(a.getCurrentLng())
                .licensePlate(a.getLicensePlate())
                .status(a.getStatus() != null ? a.getStatus() : "AVAILABLE")
                .build();
    }

    private EmergencyInterventionResponseDTO toInterventionDTO(EmergencyIntervention i) {
        return EmergencyInterventionResponseDTO.builder()
                .id(i.getId())
                .emergencyAlertId(i.getEmergencyAlert().getId())
                .clinicId(i.getClinic().getId())
                .ambulanceId(i.getAmbulance().getId())
                .patientName(i.getEmergencyAlert().getDevice().getPatient().getFullName())
                .status(i.getStatus())
                .dispatchedAt(i.getDispatchedAt())
                .arrivedAt(i.getArrivedAt())
                .completedAt(i.getCompletedAt())
                .build();
    }
}