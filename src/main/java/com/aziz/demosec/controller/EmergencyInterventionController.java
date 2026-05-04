// EmergencyInterventionController.java
package com.aziz.demosec.controller;

import com.aziz.demosec.Entities.EmergencyInterventionStatus;
import com.aziz.demosec.dto.emergency.*;
import com.aziz.demosec.service.IEmergencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/interventions")
@RequiredArgsConstructor
public class EmergencyInterventionController {

    private final IEmergencyService emergencyService;

    @PostMapping("/dispatch")
    public ResponseEntity<EmergencyInterventionResponseDTO> dispatch(
            @Valid @RequestBody EmergencyInterventionRequestDTO dto) {
        return ResponseEntity.ok(emergencyService.dispatchIntervention(dto));
    }

    @PostMapping("/auto-dispatch/{alertId}")
    public ResponseEntity<AutoDispatchResponseDTO> autoDispatch(@PathVariable Long alertId) {
        return ResponseEntity.ok(emergencyService.autoDispatch(alertId));
    }

    @GetMapping
    public ResponseEntity<List<EmergencyInterventionResponseDTO>> getAll() {
        return ResponseEntity.ok(emergencyService.getAllInterventions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmergencyInterventionResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(emergencyService.getInterventionById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<EmergencyInterventionResponseDTO> updateStatus(
            @PathVariable Long id, @RequestParam EmergencyInterventionStatus status) {
        return ResponseEntity.ok(emergencyService.updateInterventionStatus(id, status));
    }
}