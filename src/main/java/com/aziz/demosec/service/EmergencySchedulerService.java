package com.aziz.demosec.service;

import com.aziz.demosec.Entities.EmergencyAlert;
import com.aziz.demosec.Entities.EmergencyAlertStatus;
import com.aziz.demosec.repository.EmergencyAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service that auto-cancels emergency alerts left unhandled
 * for more than EXPIRY_MINUTES without any intervention dispatched.
 *
 * Query uses 3 explicit JOINs:
 *   JOIN 1 : EmergencyAlert → SmartDevice          (ea.device)
 *   JOIN 2 : SmartDevice    → Patient               (sd.patient)
 *   JOIN 3 : EmergencyAlert → EmergencyIntervention (ea.intervention, LEFT JOIN)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencySchedulerService {

    private static final int EXPIRY_MINUTES = 5;

    private static final List<EmergencyAlertStatus> PENDING_STATUSES = List.of(
        EmergencyAlertStatus.PENDING,
        EmergencyAlertStatus.CLINIC_NOTIFIED,
        EmergencyAlertStatus.PATIENT_NOTIFIED,
        EmergencyAlertStatus.CONTACT_NOTIFIED
    );

    private final EmergencyAlertRepository alertRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelExpiredAlerts() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRY_MINUTES);

        List<EmergencyAlert> expired = alertRepository.findExpiredUnhandledAlerts(
            PENDING_STATUSES, cutoff
        );

        if (expired.isEmpty()) return;

        for (EmergencyAlert alert : expired) {
            alert.setStatus(EmergencyAlertStatus.CANCELED);
            alert.setCanceledByPatient(false);
        }
        alertRepository.saveAll(expired);

        log.info("[Scheduler] Auto-canceled {} emergency alert(s) unhandled for over {} minutes",
            expired.size(), EXPIRY_MINUTES);
    }
}
