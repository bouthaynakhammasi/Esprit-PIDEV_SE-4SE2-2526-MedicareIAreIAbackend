
package com.aziz.demosec.controller;

import com.aziz.demosec.dto.emergency.*;
import com.aziz.demosec.service.IEmergencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/ambulances")
@RequiredArgsConstructor
public class AmbulanceController {

    private final IEmergencyService emergencyService;

    @PostMapping
    public ResponseEntity<AmbulanceResponseDTO> create(@Valid @RequestBody AmbulanceRequestDTO dto) {
        return ResponseEntity.ok(emergencyService.createAmbulance(dto));
    }

    @GetMapping
    public ResponseEntity<List<AmbulanceResponseDTO>> getAll() {
        return ResponseEntity.ok(emergencyService.getAllAmbulances());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AmbulanceResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(emergencyService.getAmbulanceById(id));
    }

    @GetMapping("/clinic/{clinicId}")
    public ResponseEntity<List<AmbulanceResponseDTO>> getByClinic(@PathVariable Long clinicId) {
        return ResponseEntity.ok(emergencyService.getAmbulancesByClinic(clinicId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AmbulanceResponseDTO> update(
            @PathVariable Long id, @RequestBody AmbulanceRequestDTO dto) {
        return ResponseEntity.ok(emergencyService.updateAmbulance(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        emergencyService.deleteAmbulance(id);
        return ResponseEntity.noContent().build();
    }
}