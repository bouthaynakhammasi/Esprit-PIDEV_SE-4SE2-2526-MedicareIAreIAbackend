package com.aziz.demosec.controller;

import com.aziz.demosec.Entities.LabResult;
import com.aziz.demosec.repository.LabResultRepository;
import com.aziz.demosec.service.LabNarratorScheduler;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lab-narrator")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Transactional
public class LabNarratorController {

    private final LabResultRepository  labResultRepository;
    private final LabNarratorScheduler labNarratorScheduler;

    // GET /api/lab-narrator/pending — results waiting for narrative
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPending() {
        LocalDateTime since = LocalDate.now().minusDays(7).atStartOfDay();
        List<LabResult> pending = labResultRepository.findResultsNeedingNarrative(since);

        Map<String, Object> response = new HashMap<>();
        response.put("count", pending.size());
        response.put("results", pending.stream().map(r -> Map.of(
                "id",          r.getId(),
                "patientName", r.getLabRequest().getPatient().getFullName(),
                "aiRisk",      r.getAiRisk() != null ? r.getAiRisk() : "—",
                "aiDiagnostic",r.getAiDiagnostic() != null ? r.getAiDiagnostic() : "—",
                "completedAt", r.getCompletedAt().toString()
        )).toList());

        return ResponseEntity.ok(response);
    }

    // POST /api/lab-narrator/generate/{id} — manually trigger for one result
    @PostMapping("/generate/{labResultId}")
    public ResponseEntity<Map<String, Object>> generateOne(@PathVariable Long labResultId) {
        return labResultRepository.findById(labResultId).map(result -> {
            labNarratorScheduler.processOneResult(result);

            Map<String, Object> response = new HashMap<>();
            response.put("success",         true);
            response.put("patientNarrative",result.getPatientNarrative());
            response.put("doctorNarrative", result.getDoctorNarrative());
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    // POST /api/lab-narrator/regenerate/{id} — force regenerate even if already generated
    @PostMapping("/regenerate/{labResultId}")
    public ResponseEntity<Map<String, Object>> regenerateOne(@PathVariable Long labResultId) {
        return labResultRepository.findById(labResultId).map(result -> {
            result.setNarrativeGenerated(false);
            labResultRepository.save(result);
            labNarratorScheduler.processOneResult(result);

            Map<String, Object> response = new HashMap<>();
            response.put("success",          true);
            response.put("patientNarrative", result.getPatientNarrative());
            response.put("doctorNarrative",  result.getDoctorNarrative());
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    // GET /api/lab-narrator/history — all narrated results
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getHistory() {
        List<LabResult> narrated = labResultRepository.findByNarrativeGeneratedTrueOrderByCompletedAtDesc();

        List<Map<String, Object>> response = narrated.stream().map(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id",               r.getId());
            item.put("patientName",      r.getLabRequest().getPatient().getFullName());
            item.put("aiRisk",           r.getAiRisk() != null ? r.getAiRisk() : "—");
            item.put("aiDiagnostic",     r.getAiDiagnostic() != null ? r.getAiDiagnostic() : "—");
            item.put("completedAt",      r.getCompletedAt().toString());
            item.put("patientNarrative", r.getPatientNarrative());
            item.put("doctorNarrative",  r.getDoctorNarrative());
            return item;
        }).toList();

        return ResponseEntity.ok(response);
    }
}
