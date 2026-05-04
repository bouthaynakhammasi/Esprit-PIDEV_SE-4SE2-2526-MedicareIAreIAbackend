package com.aziz.demosec.Schedulers;

import com.aziz.demosec.Entities.Patient;
import com.aziz.demosec.repository.*;
import com.aziz.demosec.service.DailyHealthReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyHealthScheduler {

    private final PatientRepository patientRepository;
    private final DailyHealthReportService reportService;

    // Runs every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void runDailyHealthCheck() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Running daily health check for date: {}", yesterday);

        List<Patient> patients = patientRepository.findAll();

        for (Patient patient : patients) {
            try {
                reportService.generateReport(patient, yesterday);
            } catch (Exception e) {
                log.error("Error processing patient {}: {}", patient.getId(), e.getMessage());
            }
        }

        log.info("Daily health check complete for {} patients", patients.size());
    }
}