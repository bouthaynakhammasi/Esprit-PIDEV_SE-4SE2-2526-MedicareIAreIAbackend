package com.aziz.demosec.controller;

import com.aziz.demosec.dto.LaboratoryRequest;
import com.aziz.demosec.dto.LaboratoryResponse;
import com.aziz.demosec.service.LaboratoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/laboratories")
@RequiredArgsConstructor
public class LaboratoryController {

    private final LaboratoryService laboratoryService;

    @GetMapping("/me")
    public ResponseEntity<LaboratoryResponse> getMyLaboratory(Authentication authentication) {
        return ResponseEntity.ok(laboratoryService.getMyLaboratory(authentication.getName()));
    }

    @GetMapping("/profile")
    public ResponseEntity<LaboratoryResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(laboratoryService.getMyLaboratory(authentication.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<LaboratoryResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody com.aziz.demosec.dto.LaboratoryStaffProfileUpdateRequest request) {
        return ResponseEntity.ok(laboratoryService.updateProfile(authentication.getName(), request));
    }

    @PostMapping
    public ResponseEntity<LaboratoryResponse> create(@Valid @RequestBody LaboratoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(laboratoryService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LaboratoryResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(laboratoryService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<LaboratoryResponse>> getAll() {
        return ResponseEntity.ok(laboratoryService.getAll());
    }

    @GetMapping("/search")
    public ResponseEntity<List<LaboratoryResponse>> searchByName(@RequestParam("name") String name) {
        return ResponseEntity.ok(laboratoryService.searchByName(name));
    }

    @GetMapping("/active")
    public ResponseEntity<List<LaboratoryResponse>> getActive() {
        return ResponseEntity.ok(laboratoryService.getActive());
    }

    @PutMapping("/{id}")
    public ResponseEntity<LaboratoryResponse> update(@PathVariable("id") Long id,
                                                     @Valid @RequestBody LaboratoryRequest request) {
        return ResponseEntity.ok(laboratoryService.update(id, request));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<LaboratoryResponse> toggleActive(@PathVariable("id") Long id) {
        return ResponseEntity.ok(laboratoryService.toggleActive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        laboratoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}