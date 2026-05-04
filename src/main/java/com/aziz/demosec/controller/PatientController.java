package com.aziz.demosec.controller;

import com.aziz.demosec.Entities.*;
import com.aziz.demosec.dto.*;
import com.aziz.demosec.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PatientController {

    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ConsultationRepository consultationRepository;
    private final TreatmentRepository treatmentRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final LifestyleGoalRepository lifestyleGoalRepository;
    private final ProgressTrackingRepository progressTrackingRepository;

    @GetMapping
    public ResponseEntity<List<PatientProfileResponse>> getAll() {
        return ResponseEntity.ok(patientRepository.findAll().stream()
                .map(this::mapPatientToResponse)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientProfileResponse> getPatientById(@PathVariable Long id) {
        return patientRepository.findById(id)
                .map(this::mapPatientToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<PatientProfileResponse> getMyProfile(java.security.Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return patientRepository.findByEmail(principal.getName())
                .map(this::mapPatientToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my-patients")
    public ResponseEntity<List<PatientProfileResponse>> getMyPatients(java.security.Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return userRepository.findByEmail(principal.getName())
                .map(doctor -> {
                    List<PatientProfileResponse> patients = appointmentRepository.findDistinctPatientsByProviderId(doctor.getId())
                            .stream()
                            .filter(user -> user instanceof Patient)
                            .map(user -> mapPatientToResponse((Patient) user))
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(patients);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private PatientProfileResponse mapPatientToResponse(Patient patient) {
        Long id = patient.getId();
        List<String> allergies = Collections.emptyList();
        List<String> diseases = Collections.emptyList();
        List<ConsultationResponse> consultations = Collections.emptyList();
        List<TreatmentResponse> treatments = Collections.emptyList();
        List<PrescriptionResponse> prescriptions = Collections.emptyList();
        List<DiagnosisResponse> diagnoses = Collections.emptyList();

        if (patient.getAllergies() != null && !patient.getAllergies().isEmpty()) {
            allergies = Arrays.asList(patient.getAllergies().split(",")).stream().map(String::trim).collect(Collectors.toList());
        }

        if (patient.getDiseases() != null && !patient.getDiseases().isEmpty()) {
            diseases = Arrays.asList(patient.getDiseases().split(",")).stream().map(String::trim).collect(Collectors.toList());
        }

        MedicalRecord record = medicalRecordRepository.findByPatientId(id).orElse(null);
        if (record != null) {
            Long recordId = record.getId();

            consultations = consultationRepository.findByMedicalRecordId(recordId).stream()
                    .map(c -> {
                        ConsultationResponse resp = ConsultationResponse.builder()
                                .id(c.getId())
                                .medicalRecordId(c.getMedicalRecord().getId())
                                .doctorId(c.getDoctor().getId())
                                .doctorName(c.getDoctor().getFullName())
                                .date(c.getDate())
                                .observations(c.getObservations())
                                .notes(c.getNotes())
                                .height(patient.getHeight())
                                .weight(patient.getWeight())
                                .status("COMPLETED")
                                .build();

                        doctorRepository.findById(c.getDoctor().getId()).ifPresent(doctor -> {
                            resp.setSpecialty(doctor.getSpecialty());
                        });

                        if (resp.getSpecialty() == null) {
                            resp.setSpecialty("General Practitioner");
                        }

                        return resp;
                    })
                    .collect(Collectors.toList());

            treatments = treatmentRepository.findByConsultationMedicalRecordId(recordId).stream()
                    .map(t -> TreatmentResponse.builder()
                            .id(t.getId())
                            .consultationId(t.getConsultation().getId())
                            .treatmentType(t.getTreatmentType())
                            .description(t.getDescription())
                            .dosage("As prescribed")
                            .startDate(t.getStartDate())
                            .endDate(t.getEndDate())
                            .status(t.getStatus())
                            .build())
                    .collect(Collectors.toList());

            prescriptions = prescriptionRepository.findByConsultationMedicalRecordId(recordId).stream()
                    .map(p -> {
                        PrescriptionResponse resp = PrescriptionResponse.builder()
                                .id(p.getId())
                                .consultationId(p.getConsultation().getId())
                                .date(p.getDate())
                                .expiryDate(p.getExpiryDate() != null ? p.getExpiryDate() : p.getDate().plusDays(7))
                                .status(p.getStatus() != null ? p.getStatus().name() : "ACTIVE")
                                .doctorName(p.getConsultation().getDoctor().getFullName())
                                .items(p.getItems().stream()
                                        .map(i -> PrescriptionItemResponse.builder()
                                                .id(i.getId())
                                                .medicationName(i.getMedicationName())
                                                .dosage(i.getDosage())
                                                .frequency(i.getFrequency())
                                                .duration(i.getDuration())
                                                .build())
                                        .collect(Collectors.toList()))
                                .build();

                        doctorRepository.findById(p.getConsultation().getDoctor().getId()).ifPresent(doctor -> {
                            resp.setDoctorSpecialty(doctor.getSpecialty());
                        });

                        diagnosisRepository.findByConsultationId(p.getConsultation().getId()).stream().findFirst().ifPresent(d -> {
                            resp.setDiagnosis(d.getDescription());
                        });

                        if (resp.getDiagnosis() == null) {
                            resp.setDiagnosis(p.getConsultation().getObservations());
                        }

                        if (p.getItems() != null && !p.getItems().isEmpty()) {
                            PrescriptionItem firstItem = p.getItems().get(0);
                            resp.setMedication(firstItem.getMedicationName());
                            resp.setDosage(firstItem.getDosage());
                            resp.setInstructions(firstItem.getFrequency() + " for " + firstItem.getDuration());
                        }
                        return resp;
                    })
                    .collect(Collectors.toList());

            diagnoses = diagnosisRepository.findByConsultationMedicalRecordId(recordId).stream()
                    .map(d -> DiagnosisResponse.builder()
                            .id(d.getId())
                            .consultationId(d.getConsultation().getId())
                            .description(d.getDescription())
                            .type(d.getType())
                            .build())
                    .collect(Collectors.toList());
        }

        List<LifestyleGoalResponse> lifestyleGoals = lifestyleGoalRepository.findByPatientId(id).stream()
                .map(com.aziz.demosec.Mapper.LifestyleGoalMapper::toGoalResponse)
                .collect(Collectors.toList());

        List<LifestylePlanResponse> lifestylePlans = lifestyleGoals.stream()
                .flatMap(g -> g.getPlans().stream())
                .collect(Collectors.toList());

        List<ProgressTrackingResponse> progressTrackings = progressTrackingRepository.findByPatientId(id).stream()
                .map(com.aziz.demosec.Mapper.ProgressTrackingMapper::toResponse)
                .collect(Collectors.toList());

        PatientProfileResponse response = new PatientProfileResponse(
                patient.getId(),
                patient.getFullName(),
                patient.getEmail(),
                patient.getPhone(),
                patient.getBirthDate(),
                patient.getGender() != null ? patient.getGender().name() : null,
                patient.getBloodType() != null ? patient.getBloodType().name() : null,
                patient.getGlucoseRate(),
                patient.getHeight(),
                patient.getWeight(),
                patient.getPhoto(),
                patient.getEmergencyContactName(),
                patient.getEmergencyContactPhone(),
                record != null,
                allergies,
                diseases,
                consultations,
                treatments,
                prescriptions,
                diagnoses,
                lifestyleGoals,
                lifestylePlans,
                progressTrackings
        );

        return response;
    }

    @PutMapping("/profile")
    public ResponseEntity<PatientProfileResponse> updateProfile(java.security.Principal principal, @Valid @RequestBody PatientProfileUpdateRequest request) {
        if (principal == null) return ResponseEntity.status(401).build();
        return patientRepository.findByEmail(principal.getName())
                .map(patient -> {
                    if (request.fullName() != null) patient.setFullName(request.fullName());
                    if (request.phone() != null) patient.setPhone(request.phone());
                    if (request.birthDate() != null) patient.setBirthDate(request.birthDate());
                    if (request.gender() != null) patient.setGender(Gender.valueOf(request.gender()));
                    if (request.bloodType() != null) patient.setBloodType(BloodType.valueOf(request.bloodType()));
                    if (request.emergencyContactName() != null) patient.setEmergencyContactName(request.emergencyContactName());
                    if (request.emergencyContactPhone() != null) patient.setEmergencyContactPhone(request.emergencyContactPhone());
                    if (request.height() != null) patient.setHeight(request.height());
                    if (request.weight() != null) patient.setWeight(request.weight());
                    if (request.allergies() != null) patient.setAllergies(request.allergies());
                    if (request.diseases() != null) patient.setDiseases(request.diseases());
                    if (request.photo() != null) patient.setPhoto(request.photo());

                    Patient saved = patientRepository.save(patient);
                    return ResponseEntity.ok(mapPatientToResponse(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}