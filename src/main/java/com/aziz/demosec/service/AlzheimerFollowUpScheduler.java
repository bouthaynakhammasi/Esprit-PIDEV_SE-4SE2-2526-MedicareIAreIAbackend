package com.aziz.demosec.service;

import com.aziz.demosec.Entities.LabRequest;
import com.aziz.demosec.Entities.LabRequestStatus;
import com.aziz.demosec.Entities.LabResult;
import com.aziz.demosec.dto.RequestedBy;
import com.aziz.demosec.repository.LabRequestRepository;
import com.aziz.demosec.repository.LabResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlzheimerFollowUpScheduler {

    private final LabResultRepository  labResultRepository;
    private final LabRequestRepository labRequestRepository;
    private final EmailService          emailService;

    // Every 1 hour
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void checkAlzheimerFollowUp() {

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🧠 [ALZHEIMER FOLLOW-UP] Scheduler START — {}", LocalDateTime.now());

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        log.info("🔍 [ALZHEIMER FOLLOW-UP] Looking for URGENT/ATTENTION results since {}", since);

        List<LabResult> atRisk = labResultRepository.findAtRiskWithoutFollowUp(since);

        if (atRisk.isEmpty()) {
            log.info("✅ [ALZHEIMER FOLLOW-UP] No at-risk patients needing follow-up");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }

        log.info("⚠️  [ALZHEIMER FOLLOW-UP] {} patient(s) need follow-up", atRisk.size());

        for (LabResult result : atRisk) {
            LabRequest original = result.getLabRequest();
            String patientName  = original.getPatient().getFullName();
            String risk         = result.getAiRisk();

            log.info("   👤 Patient: {} | Risk: {} | Result date: {}",
                    patientName, risk, result.getCompletedAt());

            // ── Create automatic follow-up request ──
            LabRequest followUp = LabRequest.builder()
                    .patient(original.getPatient())
                    .doctor(original.getDoctor())
                    .laboratory(original.getLaboratory())
                    .testType("Alzheimer Follow-up")
                    .status(LabRequestStatus.PENDING)
                    .requestedBy(RequestedBy.DOCTOR)
                    .clinicalNotes("Automatic follow-up — patient had " + risk
                            + " result on " + result.getCompletedAt().toLocalDate()
                            + ". AI diagnostic: " + result.getAiDiagnostic())
                    .requestedAt(LocalDateTime.now())
                    .doctorEmail(original.getDoctorEmail())
                    .notificationSent(false)
                    .build();

            labRequestRepository.save(followUp);
            log.info("   ✅ Follow-up request created for patient: {}", patientName);

            // ── Notify doctor by email ──
            if (original.getDoctorEmail() != null && !original.getDoctorEmail().isBlank()) {
                try {
                    emailService.sendFollowUpAlert(
                            original.getDoctorEmail(),
                            patientName,
                            risk,
                            result.getAiDiagnostic(),
                            result.getCompletedAt().toLocalDate().toString()
                    );
                    log.info("   📧 Doctor notified at: {}", original.getDoctorEmail());
                } catch (Exception e) {
                    log.error("   ❌ Email failed for {}: {}", original.getDoctorEmail(), e.getMessage());
                }
            } else {
                log.warn("   ⚠️  No doctor email for patient: {}", patientName);
            }
        }

        log.info("🏁 [ALZHEIMER FOLLOW-UP] {} follow-up(s) created", atRisk.size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
