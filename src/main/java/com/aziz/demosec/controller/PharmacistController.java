package com.aziz.demosec.controller;

import com.aziz.demosec.Entities.Pharmacist;
import com.aziz.demosec.Entities.PharmacistStatus;
import com.aziz.demosec.repository.PharmacistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pharmacist")
@RequiredArgsConstructor
public class PharmacistController {

    private final PharmacistRepository pharmacistRepository;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<?> getCurrentPharmacist(Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Pharmacist> pharmacistOpt = pharmacistRepository.findByEmail(email);
            if (pharmacistOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Pharmacist not found"));
            }

            Pharmacist p = pharmacistOpt.get();
            return ResponseEntity.ok(Map.of(
                    "id", p.getId() != null ? p.getId() : "",
                    "fullName", p.getFullName() != null ? p.getFullName() : "",
                    "email", p.getEmail() != null ? p.getEmail() : "",
                    "pharmacyName", p.getPharmacyName() != null ? p.getPharmacyName() : "",
                    "pharmacyAddress", p.getPharmacyAddress() != null ? p.getPharmacyAddress() : "",
                    "pharmacySetupCompleted", p.isPharmacySetupCompleted(),
                    "status", p.getStatus() != null ? p.getStatus().name() : "PENDING"
            ));
        } catch (Exception e) {
            String cause = e.getCause() != null ? e.getCause().getMessage() : "None";
            return ResponseEntity.status(500).body(Map.of("error", "Server Error: " + e.getMessage() + " | Cause: " + cause));
        }
    }

    @PostMapping(value = "/setup-pharmacy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PHARMACIST')")
    public ResponseEntity<?> setupPharmacy(
            @RequestParam("pharmacy_name") String pharmacyName,
            @RequestParam("pharmacy_address") String pharmacyAddress,
            @RequestParam("pharmacy_phone") String pharmacyPhone,
            @RequestParam("pharmacy_email") String pharmacyEmail,
            @RequestParam("pharmacy_latitude") Float pharmacyLatitude,
            @RequestParam("pharmacy_longitude") Float pharmacyLongitude,
            @RequestParam(value = "diploma_document", required = false) MultipartFile diplomaDocument,
            Authentication authentication) {

        String email = authentication.getName();
        Optional<Pharmacist> pharmacistOpt = pharmacistRepository.findByEmail(email);
        if (pharmacistOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Pharmacist not found"));
        }

        Pharmacist pharmacist = pharmacistOpt.get();
        pharmacist.setPharmacyName(pharmacyName);
        pharmacist.setPharmacyAddress(pharmacyAddress);
        pharmacist.setPharmacyPhone(pharmacyPhone);
        pharmacist.setPharmacyEmail(pharmacyEmail);
        pharmacist.setPharmacyLatitude(pharmacyLatitude);
        pharmacist.setPharmacyLongitude(pharmacyLongitude);
        pharmacist.setPharmacySetupCompleted(true);
        pharmacist.setStatus(PharmacistStatus.PENDING);

        try {
            if (diplomaDocument != null && !diplomaDocument.isEmpty()) {
                String base64Image = Base64.getEncoder().encodeToString(diplomaDocument.getBytes());
                pharmacist.setDiplomaDocument("data:" + diplomaDocument.getContentType() + ";base64," + base64Image);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to process document"));
        }

        pharmacistRepository.save(pharmacist);

        return ResponseEntity.ok(Map.of("success", true, "message", "Your request has been submitted for admin review."));
    }
}
