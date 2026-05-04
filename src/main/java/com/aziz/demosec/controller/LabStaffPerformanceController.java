package com.aziz.demosec.controller;

import com.aziz.demosec.dto.LabStaffPerformanceDTO;
import com.aziz.demosec.repository.LabResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lab-staff/performance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LabStaffPerformanceController {

    private final LabResultRepository labResultRepository;

    /**
     * GET /api/lab-staff/performance?from=2025-01-01&to=2025-01-31
     * If no params → defaults to yesterday
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPerformance(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate resolvedFrom = (from != null) ? from : LocalDate.now().minusDays(1);
        LocalDate resolvedTo   = (to   != null) ? to   : LocalDate.now();

        LocalDateTime dtFrom = resolvedFrom.atStartOfDay();
        LocalDateTime dtTo   = resolvedTo.atStartOfDay().plusDays(1).minusSeconds(1);

        List<LabStaffPerformanceDTO> stats      = labResultRepository.findStaffPerformance(dtFrom, dtTo);
        Long                         total      = labResultRepository.countAnalysesBetween(dtFrom, dtTo);
        Long                         urgent     = labResultRepository.countUrgentBetween(dtFrom, dtTo);

        Map<String, Object> response = new HashMap<>();
        response.put("from",           resolvedFrom.toString());
        response.put("to",             resolvedTo.toString());
        response.put("totalAnalyses",  total);
        response.put("urgentCount",    urgent);
        response.put("technicianCount",stats.size());
        response.put("staff",          stats);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/lab-staff/performance/today
     */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getToday() {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to   = LocalDate.now().atTime(23, 59, 59);

        List<LabStaffPerformanceDTO> stats  = labResultRepository.findStaffPerformance(from, to);
        Long                         total  = labResultRepository.countAnalysesBetween(from, to);
        Long                         urgent = labResultRepository.countUrgentBetween(from, to);

        Map<String, Object> response = new HashMap<>();
        response.put("date",           LocalDate.now().toString());
        response.put("totalAnalyses",  total);
        response.put("urgentCount",    urgent);
        response.put("technicianCount",stats.size());
        response.put("staff",          stats);

        return ResponseEntity.ok(response);
    }
}
