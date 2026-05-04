package com.aziz.demosec.service;

import com.aziz.demosec.Entities.*;
import com.aziz.demosec.domain.Role;
import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.AuthResponse;
import com.aziz.demosec.dto.LoginRequest;
import com.aziz.demosec.dto.RegisterRequest;
import com.aziz.demosec.repository.*;
import com.aziz.demosec.security.CustomUserDetailsService;
import com.aziz.demosec.security.jwt.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IAuthServiceImp implements IAuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ClinicRepository clinicRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PharmacistRepository pharmacistRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final LaboratoryStaffRepository laboratoryStaffRepository;
    private final NutritionistRepository nutritionistRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final HomeCareServiceRepository homeCareServiceRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalHistoryRepository medicalHistoryRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    public IAuthServiceImp(
            UserRepository userRepository,
            PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            ClinicRepository clinicRepository,
            PharmacyRepository pharmacyRepository,
            PharmacistRepository pharmacistRepository,
            LaboratoryRepository laboratoryRepository,
            LaboratoryStaffRepository laboratoryStaffRepository,
            NutritionistRepository nutritionistRepository,
            ServiceProviderRepository serviceProviderRepository,
            HomeCareServiceRepository homeCareServiceRepository,
            MedicalRecordRepository medicalRecordRepository,
            MedicalHistoryRepository medicalHistoryRepository,
            @org.springframework.context.annotation.Lazy PasswordEncoder passwordEncoder,
            @org.springframework.context.annotation.Lazy AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.clinicRepository = clinicRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.pharmacistRepository = pharmacistRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.laboratoryStaffRepository = laboratoryStaffRepository;
        this.nutritionistRepository = nutritionistRepository;
        this.serviceProviderRepository = serviceProviderRepository;
        this.homeCareServiceRepository = homeCareServiceRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.medicalHistoryRepository = medicalHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public User register(RegisterRequest req) {
        log.info("Processing registration for email: {} with role: {}", req.getEmail(), req.getRole());

        if (req.getEmail() == null || req.getEmail().isBlank())
            throw new IllegalArgumentException("Email required");
        if (req.getPassword() == null || req.getPassword().length() < 8)
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        if (req.getRole() == null)
            throw new IllegalArgumentException("Role required");
        if (userRepository.findByEmail(req.getEmail()).isPresent())
            throw new IllegalArgumentException("Email already used");

        Role role;
        try {
            role = Role.valueOf(req.getRole());
        } catch (Exception e) {
            log.error("Invalid role provided: {}", req.getRole());
            throw new IllegalArgumentException("Invalid role");
        }

        User user;
        switch (role) {
            case PATIENT:
                user = registerPatient(req);
                break;
            case DOCTOR:
                user = registerDoctor(req);
                break;
            case CLINIC:
                user = registerClinic(req);
                break;
            case PHARMACIST:
                user = registerPharmacist(req);
                break;
            case LABORATORYSAFF:
                user = registerLabStaff(req);
                break;
            case NUTRITIONIST:
                user = registerNutritionist(req);
                break;
            case HOME_CARE_PROVIDER:
                user = registerHomeCareProvider(req);
                break;
            default:
                user = registerGenericUser(req);
                break;
        }

        return user;
    }

    private User registerPatient(RegisterRequest req) {
        Patient patient = new Patient();
        mapCommonFields(patient, req);

        if (req.getGender() != null) {
            try { patient.setGender(Gender.valueOf(req.getGender())); } catch (Exception ignored) {}
        }
        if (req.getBloodType() != null) {
            try { patient.setBloodType(BloodType.valueOf(req.getBloodType())); } catch (Exception ignored) {}
        }

        patient.setEmergencyContactName(req.getEmergencyContactName());
        patient.setEmergencyContactPhone(req.getEmergencyContactPhone());
        // patient.setProfileCompleted(true);
        patient = patientRepository.save(patient);

        // Medical Record and History
        MedicalRecord record = MedicalRecord.builder().patient(patient).build();
        record = medicalRecordRepository.save(record);

        if (req.getMedicalHistories() != null) {
            MedicalRecord finalRecord = record;
            List<MedicalHistory> histories = req.getMedicalHistories().stream().map(h -> {
                try {
                    return MedicalHistory.builder()
                            .medicalRecord(finalRecord)
                            .type(MedicalHistoryType.valueOf(h.getType()))
                            .description(h.getDescription())
                            .status(MedicalHistoryStatus.ACTIVE)
                            .build();
                } catch (Exception e) {
                    log.warn("Failed to map medical history entry: {}", h);
                    return null;
                }
            }).filter(h -> h != null).collect(Collectors.toList());

            if (!histories.isEmpty()) {
                medicalHistoryRepository.saveAll(histories);
            }
        }

        return patient;
    }

    private User registerDoctor(RegisterRequest req) {
        Doctor doctor = new Doctor();
        mapCommonFields(doctor, req);
        doctor.setSpecialty(req.getSpecialty());
        doctor.setLicenseNumber(req.getLicenseNumber() == null ? "LIC-" + System.currentTimeMillis() : req.getLicenseNumber());
        doctor.setConsultationFee(req.getConsultationFee() != null ? BigDecimal.valueOf(req.getConsultationFee()) : BigDecimal.ZERO);

        if (req.getConsultationMode() != null) {
            try { doctor.setConsultationMode(ConsultationMode.valueOf(req.getConsultationMode())); }
            catch (Exception e) { doctor.setConsultationMode(ConsultationMode.BOTH); }
        } else {
            doctor.setConsultationMode(ConsultationMode.BOTH);
        }

        // doctor.setClinicAddress(req.getClinicAddress());

        // if (req.getClinicId() != null) {
        //     clinicRepository.findById(req.getClinicId()).ifPresent(doctor::setClinic);
        // }

        // doctor.setProfileCompleted(true);
        return doctorRepository.save(doctor);
    }

    private User registerClinic(RegisterRequest req) {
        User user = registerGenericUser(req);
        // user.setProfileCompleted(true);
        user = userRepository.save(user);

        Clinic clinic = new Clinic();
        clinic.setName(req.getClinicName() == null ? "Unnamed Clinic" : req.getClinicName());
        clinic.setAddress(req.getClinicAddress());
        clinic.setPhone(req.getClinicPhone());
        clinic.setEmergencyPhone(req.getEmergencyPhone());
        clinic.setAmbulancePhone(req.getAmbulancePhone());
        clinic.setVerified(false);
        clinic.setUserId(user.getId());
        clinicRepository.save(clinic);

        return user;
    }

    private User registerPharmacist(RegisterRequest req) {
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setName(req.getPharmacyName() == null ? "Unnamed Pharmacy" : req.getPharmacyName());
        pharmacy.setAddress(req.getPharmacyAddress());
        pharmacy.setPhoneNumber(req.getPharmacyPhone());
        pharmacy.setEmail(req.getPharmacyEmail());
        pharmacy = pharmacyRepository.save(pharmacy);

        Pharmacist pharmacist = new Pharmacist();
        mapCommonFields(pharmacist, req);
        pharmacist.setPharmacy(pharmacy);
        // pharmacist.setProfileCompleted(true);
        return pharmacistRepository.save(pharmacist);
    }

    private User registerLabStaff(RegisterRequest req) {
        Laboratory lab = new Laboratory();
        lab.setName(req.getLabName() == null ? "Unnamed Lab" : req.getLabName());
        lab.setAddress(req.getLabAddress());
        lab.setPhone(req.getLabPhone());
        lab = laboratoryRepository.save(lab);

        LaboratoryStaff staff = new LaboratoryStaff();
        mapCommonFields(staff, req);
        staff.setLaboratory(lab);
        // staff.setProfileCompleted(true);
        return laboratoryStaffRepository.save(staff);
    }

    private User registerNutritionist(RegisterRequest req) {
        Nutritionist nutr = new Nutritionist();
        mapCommonFields(nutr, req);
        nutr.setSpecialties(req.getSpecialty());
        nutr.setLicenseNumber(req.getLicenseNumber() == null ? "LIC-" + System.currentTimeMillis() : req.getLicenseNumber());
        // nutr.setVerified(false);
        // nutr.setProfileCompleted(true);
        return nutritionistRepository.save(nutr);
    }

    private User registerHomeCareProvider(RegisterRequest req) {
        User user = registerGenericUser(req);
        // user.setProfileCompleted(true);
        user = userRepository.save(user);

        ServiceProvider provider = new ServiceProvider();
        provider.setUser(user);
        provider.setCertificationDocument(req.getCertificationDocument());
        // provider.setVerified(false);

        if (req.getHomeCareServices() != null) {
            Set<HomeCareService> services = new HashSet<>();
            for (String serviceName : req.getHomeCareServices()) {
                homeCareServiceRepository.findByName(serviceName).ifPresent(services::add);
            }
            provider.setSpecialties(services);
        }

        serviceProviderRepository.save(provider);
        return user;
    }

    private User registerGenericUser(RegisterRequest req) {
        User user = new User();
        mapCommonFields(user, req);
        return userRepository.save(user);
    }

    private void mapCommonFields(User user, RegisterRequest req) {
        user.setFullName(req.getFullName() == null ? "Not Available" : req.getFullName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        try {
            user.setRole(Role.valueOf(req.getRole()));
        } catch (Exception e) {
            user.setRole(Role.VISITOR);
        }

        user.setPhone(req.getPhone());
        user.setBirthDate(req.getBirthDate() != null ? req.getBirthDate().toString() : null);
        user.setProfileImage(req.getProfileImage());
        user.setEnabled(true);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        UserDetails userDetails = userDetailsService.loadUserByUsername(req.email());
        User user = userRepository.findByEmail(req.email()).orElseThrow(() -> new RuntimeException("User not found"));
        String role = userDetails.getAuthorities().stream().findFirst().map(GrantedAuthority::getAuthority).orElse("ROLE_VISITOR");
        String token = jwtService.generateToken(userDetails, user.getFullName(), user.getId());
        return new AuthResponse(token, userDetails.getUsername(), user.getFullName(), role);
    }
}