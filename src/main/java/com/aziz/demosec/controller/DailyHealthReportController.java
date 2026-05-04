package com.aziz.demosec.controller;

import com.aziz.demosec.dto.DailyHealthReportDTO;
import com.aziz.demosec.Entities.DailyHealthReport;
import com.aziz.demosec.repository.DailyHealthReportRepository;
import com.aziz.demosec.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/health-reports")
@RequiredArgsConstructor
public class DailyHealthReportController {

    private final DailyHealthReportRepository reportRepository;
    private final UserRepository userRepository;
    private final com.aziz.demosec.repository.PatientRepository patientRepository;
    private final com.aziz.demosec.service.DailyHealthReportService reportService;

    @GetMapping("/latest-summary")
    public ResponseEntity<List<DailyHealthReportDTO>> getLatestSummary(java.security.Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return userRepository.findByEmail(principal.getName())
                .map(user -> {
                    List<DailyHealthReportDTO> reports = reportRepository.findLatestReportsByNutritionist(user.getId())
                            .stream().map(this::toDTO).collect(Collectors.toList());
                    return ResponseEntity.ok(reports);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Nutritionist: view all anomalies across all patients
    @GetMapping("/anomalies")
    public ResponseEntity<List<DailyHealthReportDTO>> getAllAnomalies() {
        return ResponseEntity.ok(
                reportRepository.findByAnomalyDetectedTrue()
                        .stream().map(this::toDTO).collect(Collectors.toList())
        );
    }

    // Patient: view own reports
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<DailyHealthReportDTO>> getPatientReports(
            @PathVariable Long patientId) {
        return ResponseEntity.ok(
                reportRepository.findByPatientId(patientId)
                        .stream().map(this::toDTO).collect(Collectors.toList())
        );
    }

    // Patient: view report for specific date
    @GetMapping("/patient/{patientId}/date/{date}")
    public ResponseEntity<DailyHealthReportDTO> getByDate(
            @PathVariable Long patientId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Optional<DailyHealthReport> report = reportRepository.findByPatientIdAndReportDate(patientId, date);

        // If it's today and not found, or if we want to force refresh today's data
        if (report.isEmpty() && date.equals(LocalDate.now())) {
            return patientRepository.findById(patientId)
                    .map(p -> reportService.generateReport(p, date))
                    .map(this::toDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        return report.map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Patient: manually trigger/refresh report generation
    @PostMapping("/patient/{patientId}/generate")
    public ResponseEntity<DailyHealthReportDTO> generate(
            @PathVariable Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        return patientRepository.findById(patientId)
                .map(p -> reportService.generateReport(p, targetDate))
                .map(this::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    // Patient: view reports over a date range
    @GetMapping("/patient/{patientId}/range")
    public ResponseEntity<List<DailyHealthReportDTO>> getRange(
            @PathVariable Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                reportRepository.findByPatientIdAndReportDateBetween(patientId, from, to)
                        .stream().map(this::toDTO).collect(Collectors.toList())
        );
    }

    private DailyHealthReportDTO toDTO(DailyHealthReport r) {
        return DailyHealthReportDTO.builder()
                .id(r.getId())
                .patientId(r.getPatient().getId())
                .patientName(r.getPatient().getFullName())
                .lifestylePlanId(r.getLifestylePlan().getId())
                .reportDate(r.getReportDate())
                .actualCalories(r.getActualCalories())
                .expectedCalories(r.getExpectedCalories())
                .calorieDifference(r.getCalorieDifference())
                .currentWeight(r.getCurrentWeight())
                .goalWeight(r.getGoalWeight())
                .weightDifference(r.getWeightDifference())
                .missedLog(r.isMissedLog())
                .anomalyDetected(r.isAnomalyDetected())
                .anomalies(r.getAnomalies())
                .patientPhoto(r.getPatient().getPhoto())
                .build();
    }
}