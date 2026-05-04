package com.aziz.demosec.dto;

import com.aziz.demosec.Entities.BloodType;
import com.aziz.demosec.Entities.Gender;
import com.aziz.demosec.Entities.ConsultationMode;
import com.aziz.demosec.domain.Role;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RegisterRequest(
        @NotNull(message = "Full name is required")
        String fullName,
        @Email(message = "Invalid email format")
        @NotNull(message = "Email is required")
        String email,
        @NotNull(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters ")
        String password,
        @NotNull(message = "Role is required")
        Role role,
        @Size(min = 8, message = "Phone number must be at least 8 characters ")
        String phone,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate,
        String profileImage,

        // Patient Fields
        Gender gender,
        BloodType bloodType,
        String emergencyContactName,
        String emergencyContactPhone,
        String glucoseRate,
        String allergies,
        String diseases,
        String chronicDiseases,
        String drugAllergies,
        String hereditaryDiseases,
        Double height,
        Double weight,

        // Provider Fields
        String specialty,
        String licenseNumber,
        BigDecimal consultationFee,
        ConsultationMode consultationMode,
        Long clinicId,

        // Clinic Fields
        String clinicName,
        String clinicAddress,
        String clinicPhone,
        String emergencyPhone,
        String ambulancePhone,

        // Pharmacy Fields
        String pharmacyName,
        String pharmacyAddress,
        String pharmacyPhone,

        Double locationLat,
        Double locationLng,

        // Laboratory Fields
        String labName,
        String labAddress,
        String labPhone,

        // Home Care Fields
        String certificationDocument,
        List<String> homeCareServices,
        List<Long> specialtyIds
) {}
