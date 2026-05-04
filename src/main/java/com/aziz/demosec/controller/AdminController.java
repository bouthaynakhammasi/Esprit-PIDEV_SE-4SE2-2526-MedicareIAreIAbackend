package com.aziz.demosec.controller;

import com.aziz.demosec.Entities.Pharmacist;
import com.aziz.demosec.Entities.PharmacistStatus;
import com.aziz.demosec.repository.PharmacistRepository;
import com.aziz.demosec.domain.Role;
import com.aziz.demosec.domain.User;
import com.aziz.demosec.repository.UserRepository;
import com.aziz.demosec.service.NotificationService;
import com.aziz.demosec.service.HomeCareManagementService;
import com.aziz.demosec.Entities.ServiceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final PharmacistRepository pharmacistRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final HomeCareManagementService homeCareService;

    // ── Pharmacist (version oussema) ─────────────────────────────────────

    @GetMapping("/pharmacists/pending")
    public ResponseEntity<List<Pharmacist>> getPendingPharmacistSetups() {
        return ResponseEntity.ok(pharmacistRepository.findByStatus(PharmacistStatus.PENDING));
    }

    @PatchMapping("/pharmacists/{id}/approve")
    public ResponseEntity<?> approvePharmacistSetup(@PathVariable("id") Long id) {
        Pharmacist pharmacist = pharmacistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pharmacist not found"));
        pharmacist.setStatus(PharmacistStatus.APPROVED);
        pharmacistRepository.save(pharmacist);
        return ResponseEntity.ok(Map.of("success", true, "message", "Pharmacist approved"));
    }

    @PatchMapping("/pharmacists/{id}/reject")
    public ResponseEntity<?> rejectPharmacistSetup(@PathVariable("id") Long id) {
        Pharmacist pharmacist = pharmacistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pharmacist not found"));
        pharmacist.setStatus(PharmacistStatus.REJECTED);
        pharmacistRepository.save(pharmacist);
        return ResponseEntity.ok(Map.of("success", true, "message", "Pharmacist rejected"));
    }

    // ── Pharmacist (version main) ─────────────────────────────────────────

    @GetMapping("/pending-pharmacists")
    public ResponseEntity<List<User>> getPendingPharmacists() {
        return ResponseEntity.ok(userRepository.findByRoleAndEnabledFalse(Role.PHARMACIST));
    }

    @PatchMapping("/approve-pharmacist/{id}")
    public ResponseEntity<Void> approvePharmacist(@PathVariable("id") Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        notificationService.notifyAccountActivated(user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/reject-pharmacist/{id}")
    public ResponseEntity<Void> rejectPharmacist(@PathVariable("id") Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.isEnabled()) {
            throw new RuntimeException("Cannot reject an already enabled user");
        }
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    // ── Home Care Provider ────────────────────────────────────────────────

    @GetMapping("/homecare/pending")
    public ResponseEntity<List<ServiceProvider>> getPendingProviders() {
        return ResponseEntity.ok(homeCareService.getPendingProviders());
    }

    @PutMapping("/homecare/approve/{id}")
    public ResponseEntity<ServiceProvider> approveProvider(@PathVariable("id") Long id) {
        return ResponseEntity.ok(homeCareService.verifyProvider(id));
    }

    @DeleteMapping("/homecare/reject/{id}")
    public ResponseEntity<Void> rejectProvider(@PathVariable("id") Long id) {
        homeCareService.rejectProvider(id);
        return ResponseEntity.noContent().build();
    }
}