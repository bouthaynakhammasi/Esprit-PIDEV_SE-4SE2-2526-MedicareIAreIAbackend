package com.aziz.demosec.service;

import com.aziz.demosec.Entities.Patient;
import com.aziz.demosec.domain.Role;
import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.AuthResponse;
import com.aziz.demosec.dto.LoginRequest;
import com.aziz.demosec.dto.RegisterRequest;
import com.aziz.demosec.Entities.*;
import com.aziz.demosec.repository.ClinicRepository;
import com.aziz.demosec.repository.PharmacistRepository;
import com.aziz.demosec.repository.*;
import com.aziz.demosec.security.CustomUserDetailsService;
import com.aziz.demosec.security.jwt.JwtService;
import java.util.Map;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IAuthServiceImp implements IAuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final LaboratoryStaffRepository laboratoryStaffRepository;
    private final DoctorRepository doctorRepository;
    private final ClinicRepository clinicRepository;
    private final PharmacistRepository pharmacistRepository;
    private final NutritionistRepository nutritionistRepository;
    private final PharmacyRepository pharmacyRepository;
    private final HomeCareServiceRepository homeCareServiceRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    public User register(RegisterRequest req) {

        if (req.email() == null || req.email().isBlank())
            throw new IllegalArgumentException("Email required");

        if (req.password() == null || req.password().length() < 8)
            throw new IllegalArgumentException("Password must contain at least 8 characters");

        if (req.role() == null)
            throw new IllegalArgumentException("Role required");

        if (userRepository.findByEmail(req.email()).isPresent())
            throw new IllegalArgumentException("Email already used");

        // =========================
        // CAS PATIENT
        // =========================
        if (req.role() == Role.PATIENT) {

            Patient patient = new Patient();
            patient.setFullName(req.fullName() == null ? "Not Available" : req.fullName());
            patient.setEmail(req.email());
            patient.setPassword(passwordEncoder.encode(req.password()));
            patient.setRole(Role.PATIENT);
            patient.setPhone(req.phone());
            patient.setBirthDate(req.birthDate());

            patient.setGender(req.gender());
            patient.setBloodType(req.bloodType());
            patient.setEmergencyContactName(req.emergencyContactName());
            patient.setEmergencyContactPhone(req.emergencyContactPhone());
            patient.setGlucoseRate(req.glucoseRate());
            patient.setAllergies(req.allergies());
            patient.setDiseases(req.diseases());
            patient.setHeight(req.height());
            patient.setWeight(req.weight());

            patient.setEnabled(true);

            return patientRepository.save(patient);
        }

        // =========================
        // OTHER ROLES
        // =========================
        User u;
        switch (req.role()) {
            case DOCTOR:
                Doctor dr = new Doctor();
                dr.setSpecialty(req.specialty());
                dr.setLicenseNumber(req.licenseNumber());
                dr.setConsultationFee(req.consultationFee());
                dr.setConsultationMode(req.consultationMode());
                u = dr;
                break;
            case CLINIC:
                Clinic cEntity = new Clinic();
                cEntity.setName(req.clinicName());
                cEntity.setAddress(req.clinicAddress());
                cEntity.setPhone(req.clinicPhone());
                cEntity.setEmergencyPhone(req.emergencyPhone());
                cEntity.setAmbulancePhone(req.ambulancePhone());
                u = cEntity;
                break;
            case PHARMACIST:
                Pharmacist pharm = new Pharmacist();

                Pharmacy phEntity = new Pharmacy();
                phEntity.setName(req.pharmacyName());
                phEntity.setAddress(req.pharmacyAddress());
                phEntity.setPhoneNumber(req.pharmacyPhone());
                phEntity.setEmail(req.pharmacyEmail());
                phEntity = pharmacyRepository.save(phEntity);

                pharm.setPharmacy(phEntity);
                u = pharm;
                break;
            case LABORATORY_STAFF:
                LaboratoryStaff labStaff = new LaboratoryStaff();

                Laboratory labEntity = new Laboratory();
                labEntity.setName(req.labName());
                labEntity.setAddress(req.labAddress());
                labEntity.setPhone(req.labPhone());
                labEntity = laboratoryRepository.save(labEntity);

                labStaff.setLaboratory(labEntity);
                u = labStaff;
                break;
            case NUTRITIONIST:
                Nutritionist nut = new Nutritionist();
                nut.setSpecialties(req.specialty());
                nut.setConsultationFee(req.consultationFee());
                nut.setLicenseNumber(req.licenseNumber());
                nut.setConsultationMode(req.consultationMode());
                u = nut;
                break;
            case HOME_CARE_PROVIDER:
                ServiceProvider sp = new ServiceProvider();
                sp.setCertificationDocument(req.certificationDocument());

                if (req.homeCareServices() != null && !req.homeCareServices().isEmpty()) {
                    java.util.Set<HomeCareService> services = new java.util.HashSet<>();
                    for (String serviceName : req.homeCareServices()) {
                        homeCareServiceRepository.findByName(serviceName)
                                .ifPresent(services::add);
                    }
                    sp.setSpecialties(services);
                }

                u = sp.getUser();
                break;
            default:
                u = new User();
                break;
        }

        u.setFullName(req.fullName() == null ? "Not Available" : req.fullName());
        u.setEmail(req.email());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setRole(req.role());

        // Only set phone if not already set specifically (e.g., for Clinic)
        if (req.role() != Role.CLINIC) {
            u.setPhone(req.phone());
        }

        u.setBirthDate(req.birthDate());
        u.setEnabled(true);

        // Use specific repositories for persistent health professionals/entities
        if (u instanceof Doctor) return doctorRepository.save((Doctor) u);
        if (u instanceof Clinic) return clinicRepository.save((Clinic) u);
        if (u instanceof Pharmacist) return pharmacistRepository.save((Pharmacist) u);
        if (u instanceof LaboratoryStaff) return laboratoryStaffRepository.save((LaboratoryStaff) u);
        if (u instanceof Nutritionist) return nutritionistRepository.save((Nutritionist) u);

        return userRepository.save(u);
    }

    @Override
    public AuthResponse login(LoginRequest req) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(req.email());

        // Fetch the user to get the fullName
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_VISITOR");

        String fullName = user.getFullName() != null ? user.getFullName() : "Unknown";
        Long userId = user.getId() != null ? user.getId() : 0L;
        String token = jwtService.generateToken(userDetails, userId, fullName);

        return new AuthResponse(token, userDetails.getUsername(), user.getFullName(), role);
    }
}