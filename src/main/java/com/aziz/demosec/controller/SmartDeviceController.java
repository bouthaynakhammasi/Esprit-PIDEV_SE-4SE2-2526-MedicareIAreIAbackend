// SmartDeviceController.java
package com.aziz.demosec.controller;

import com.aziz.demosec.dto.emergency.*;
import com.aziz.demosec.service.IEmergencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/smart-devices")
@RequiredArgsConstructor
public class SmartDeviceController {

    private final IEmergencyService emergencyService;

    @PostMapping("/create")
    public ResponseEntity<SmartDeviceResponseDTO> create(@RequestBody SmartDeviceRequestDTO dto) {
        return ResponseEntity.ok(emergencyService.createSmartDevice(dto));
    }

    @GetMapping
    public ResponseEntity<List<SmartDeviceResponseDTO>> getAll() {
        return ResponseEntity.ok(emergencyService.getAllSmartDevices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SmartDeviceResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(emergencyService.getSmartDeviceById(id));
    }

    @GetMapping("/by-patient/{patientId}")
    public ResponseEntity<SmartDeviceResponseDTO> getByPatientId(@PathVariable Long patientId) {
        return ResponseEntity.ok(emergencyService.getSmartDeviceByPatientId(patientId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        emergencyService.deleteSmartDevice(id);
        return ResponseEntity.noContent().build();
    }
}