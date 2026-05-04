package com.aziz.demosec.service;

import com.aziz.demosec.Entities.Laboratory;
import com.aziz.demosec.Entities.LaboratoryStaff;
import com.aziz.demosec.Entities.Patient;
import com.aziz.demosec.Entities.Pharmacist;
import com.aziz.demosec.Entities.Pharmacy;
import com.aziz.demosec.Entities.Doctor;
import com.aziz.demosec.Entities.Nutritionist;
import com.aziz.demosec.domain.PasswordResetToken;
import com.aziz.demosec.domain.Role;
import com.aziz.demosec.domain.User;
import com.aziz.demosec.dto.AuthResponse;
import com.aziz.demosec.dto.LoginRequest;
import com.aziz.demosec.dto.RegisterRequest;
import com.aziz.demosec.repository.LaboratoryRepository;
import com.aziz.demosec.repository.LaboratoryStaffRepository;
import com.aziz.demosec.repository.NutritionistRepository;
import com.aziz.demosec.repository.PasswordResetTokenRepository;
import com.aziz.demosec.repository.PatientRepository;
import com.aziz.demosec.repository.PharmacistRepository;
import com.aziz.demosec.repository.PharmacyRepository;
import com.aziz.demosec.repository.UserRepository;
import com.aziz.demosec.repository.DoctorRepository;
import com.aziz.demosec.security.CustomUserDetailsService;
import com.aziz.demosec.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PharmacistRepository pharmacistRepository;
    private final PharmacyRepository pharmacyRepository;
    private final DoctorRepository doctorRepository;
    private final NutritionistRepository nutritionistRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final LaboratoryStaffRepository laboratoryStaffRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final com.aziz.demosec.repository.ServiceProviderRepository serviceProviderRepository;
    private final com.aziz.demosec.repository.HomeCareServiceRepository homeCareServiceRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public User register(RegisterRequest req, String documentUrl) {
        System.out.println("Registering user: " + req.email() + " with role: " + req.role());

        if (req.email() == null || req.email().isBlank())
            throw new IllegalArgumentException("Email required");

        if (req.password() == null || req.password().length() < 8)
            throw new IllegalArgumentException("Password must contain at least 8 characters");

        if (req.role() == null)
            throw new IllegalArgumentException("Role required");

        if (userRepository.findByEmail(req.email()).isPresent())
            throw new IllegalArgumentException("Email already used");

        // ✅ Validation de la date de naissance
        if (req.birthDate() != null) {
            com.aziz.demosec.util.BirthDateValidator.validate(req.birthDate());
        }

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
        // CAS PHARMACIST (Nouveau)
        // =========================
        if (req.role() == Role.PHARMACIST) {
            if (req.pharmacyName() == null || req.pharmacyName().isBlank()) {
                throw new IllegalArgumentException("Le nom de la pharmacie est obligatoire pour un pharmacien");
            }

            // 1. Créer la Pharmacie
            Pharmacy newPharmacy = Pharmacy.builder()
                    .name(req.pharmacyName())
                    .address(req.pharmacyAddress() != null ? req.pharmacyAddress() : "")
                    .phoneNumber(req.pharmacyPhone() != null ? req.pharmacyPhone() : "")
                    .email(req.email())
                    .locationLat(req.locationLat())
                    .locationLng(req.locationLng())
                    .build();
            newPharmacy = pharmacyRepository.save(newPharmacy);

            // 2. Créer le Pharmacien
            Pharmacist pharmacist = new Pharmacist();
            pharmacist.setFullName(req.fullName() == null ? "Not Available" : req.fullName());
            pharmacist.setEmail(req.email());
            pharmacist.setPassword(passwordEncoder.encode(req.password()));
            pharmacist.setRole(Role.PHARMACIST);
            pharmacist.setPhone(req.phone());
            pharmacist.setBirthDate(req.birthDate());
            pharmacist.setProfessionalDocument(documentUrl);
            pharmacist.setEnabled(false); // ❌ Disabled until admin approval
            pharmacist.setPharmacy(newPharmacy);

            Pharmacist saved = pharmacistRepository.save(pharmacist);

            // 🔔 Notify all ADMINS
            notificationService.notifyAdminsOfNewPharmacist(saved);

            return saved;
        }

        // =========================
        // CAS USER / DOCTOR / ADMIN
        // =========================

        // =========================
        // CAS HOME_CARE_PROVIDER
        // =========================
        if (req.role() == Role.HOME_CARE_PROVIDER) {
            com.aziz.demosec.Entities.ServiceProvider provider = new com.aziz.demosec.Entities.ServiceProvider();
            User user = new User();
            user.setFullName(req.fullName() == null ? "Not Available" : req.fullName());
            user.setEmail(req.email());
            user.setPassword(passwordEncoder.encode(req.password()));
            user.setRole(Role.HOME_CARE_PROVIDER);
            user.setPhone(req.phone());
            user.setBirthDate(req.birthDate());
            user.setEnabled(false); // ❌ Set to false for admin approval

            user = userRepository.save(user);

            provider.setUser(user);
            provider.setVerified(false); // ❌ Set to false for admin approval
            provider.setCertificationDocument(documentUrl);

            // ✅ Ajouter les spécialités/services choisis
            if (req.specialtyIds() != null && !req.specialtyIds().isEmpty()) {
                java.util.List<com.aziz.demosec.Entities.HomeCareService> specialties = homeCareServiceRepository
                        .findAllById(req.specialtyIds());
                provider.setSpecialties(new java.util.HashSet<>(specialties));
            }

            com.aziz.demosec.Entities.ServiceProvider savedProvider = serviceProviderRepository.save(provider);
            notificationService.notifyAdminsOfNewProvider(savedProvider);

            return savedProvider.getUser();
        }

        // =========================
        // CAS DOCTOR
        // =========================
        if (req.role() == Role.DOCTOR) {
            Doctor doctor = new Doctor();
            doctor.setFullName(req.fullName() == null ? "Not Available" : req.fullName());
            doctor.setEmail(req.email());
            doctor.setPassword(passwordEncoder.encode(req.password()));
            doctor.setRole(Role.DOCTOR);
            doctor.setPhone(req.phone());
            doctor.setBirthDate(req.birthDate());
            doctor.setProfessionalDocument(documentUrl);
            doctor.setEnabled(true);

            // Set required Doctor-specific fields
            doctor.setLicenseNumber(req.licenseNumber() != null ? req.licenseNumber() : "PENDING_" + System.currentTimeMillis());
            doctor.setSpecialty(req.specialty());
            doctor.setConsultationFee(req.consultationFee());
            doctor.setConsultationMode(req.consultationMode() != null ? req.consultationMode() : com.aziz.demosec.Entities.ConsultationMode.BOTH);

            return doctorRepository.save(doctor);
        }

        // =========================
        // CAS NUTRITIONIST
        // =========================
        if (req.role() == Role.NUTRITIONIST) {
            Nutritionist nutritionist = new Nutritionist();
            nutritionist.setFullName(req.fullName() == null ? "Not Available" : req.fullName());
            nutritionist.setEmail(req.email());
            nutritionist.setPassword(passwordEncoder.encode(req.password()));
            nutritionist.setRole(Role.NUTRITIONIST);
            nutritionist.setPhone(req.phone());
            nutritionist.setBirthDate(req.birthDate());
            nutritionist.setProfessionalDocument(documentUrl);
            nutritionist.setEnabled(true);


            // Set required Nutritionist-specific fields
            nutritionist.setLicenseNumber(req.licenseNumber() != null ? req.licenseNumber() : "PENDING_" + System.currentTimeMillis());
            nutritionist.setConsultationFee(req.consultationFee());
            nutritionist.setConsultationMode(req.consultationMode() != null ? req.consultationMode() : com.aziz.demosec.Entities.ConsultationMode.BOTH);

            return nutritionistRepository.save(nutritionist);
        }

        // =========================
        // CAS LABORATORY_STAFF
        // =========================
        if (req.role() == Role.LABORATORY_STAFF) {
            // 1. Create Laboratory
            Laboratory lab = Laboratory.builder()
                    .name(req.pharmacyName() != null ? req.pharmacyName() : "New Laboratory") // Using pharmacyName field for convenience if needed, or fullName
                    .address(req.pharmacyAddress() != null ? req.pharmacyAddress() : "")
                    .phone(req.phone())
                    .email(req.email())
                    .active(true)
                    .build();
            lab = laboratoryRepository.save(lab);

            // 2. Create Laboratory Staff
            LaboratoryStaff staff = new LaboratoryStaff();
            staff.setFullName(req.fullName() == null ? "Not Available" : req.fullName());
            staff.setEmail(req.email());
            staff.setPassword(passwordEncoder.encode(req.password()));
            staff.setRole(Role.LABORATORY_STAFF);
            staff.setPhone(req.phone());
            staff.setBirthDate(req.birthDate());
            staff.setEnabled(true);
            staff.setLaboratory(lab);

            return laboratoryStaffRepository.save(staff);
        }

        // =========================
        // CAS USER / ADMIN
        // =========================
        User u = User.builder()
                .fullName(req.fullName() == null ? "Not Available" : req.fullName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .role(req.role())
                .phone(req.phone())
                .birthDate(req.birthDate())
                .enabled(true)
                .build();

        return userRepository.save(u);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        System.out.println("Login attempt for email: [" + req.email() + "]");

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(req.email());

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_VISITOR");

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + req.email()));

        String token = jwtService.generateToken(userDetails, user.getId(), user.getFullName());

        return new AuthResponse(token, userDetails.getUsername(), user.getFullName(), role);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email non trouvé"));

        tokenRepository.deleteByUser_Id(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        String resetLink = "http://localhost:4200/auth/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(email, resetLink);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide"));

        if (resetToken.isExpired())
            throw new IllegalArgumentException("Token expiré");

        if (resetToken.isUsed())
            throw new IllegalArgumentException("Token déjà utilisé");

        if (newPassword == null || newPassword.length() < 8)
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères");

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}