package com.aziz.demosec.service;

import com.aziz.demosec.Entities.LabResult;
import com.aziz.demosec.Entities.Patient;
import com.aziz.demosec.repository.LabResultRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LabNarratorScheduler {

    private final LabResultRepository labResultRepository;
    private final LabNarratorService  labNarratorService;
    private final EmailService        emailService;

    @Value("${twilio.account.sid}")   private String twilioSid;
    @Value("${twilio.auth.token}")    private String twilioToken;
    @Value("${twilio.whatsapp.from}") private String twilioFrom;

    // Every 3 days
    @Scheduled(fixedRate = 259_200_000)
    @Transactional
    public void generateNarratives() {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🤖 [AI NARRATOR] Scheduler START — {}", LocalDateTime.now());

        LocalDateTime since = LocalDate.now().minusDays(7).atStartOfDay();
        List<LabResult> pending = labResultRepository.findResultsNeedingNarrative(since);

        if (pending.isEmpty()) {
            log.info("✅ [AI NARRATOR] No results pending narrative generation");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }

        log.info("📋 [AI NARRATOR] {} result(s) need narrative", pending.size());
        for (LabResult result : pending) {
            processOneResult(result);
        }
        log.info("🏁 [AI NARRATOR] Done — {} narrative(s) generated", pending.size());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Transactional
    public void processOneResult(LabResult result) {
        Patient patient = result.getLabRequest().getPatient();
        log.info("   🧠 Processing: {} | Risk: {}", patient.getFullName(), result.getAiRisk());

        List<LabResult> history = labResultRepository
                .findByLabRequest_Patient_IdOrderByCompletedAtDesc(patient.getId());
        Long urgentCount = labResultRepository
                .countByLabRequest_Patient_IdAndAiRisk(patient.getId(), "URGENT");

        log.info("   📊 History: {} analyses | {} urgent", history.size(), urgentCount);
        log.info("   🤖 Calling Groq...");

        String patientText = labNarratorService.generatePatientNarrative(result, patient, history, urgentCount);
        String doctorText  = labNarratorService.generateDoctorNarrative(result, patient, history);

        result.setPatientNarrative(patientText);
        result.setDoctorNarrative(doctorText);
        result.setNarrativeGenerated(true);
        labResultRepository.save(result);
        log.info("   ✅ Narratives saved to DB");

        // ── Email to doctor (clinical language) ──
        String doctorEmail = result.getLabRequest().getDoctorEmail();
        if (doctorEmail != null && !doctorEmail.isBlank()) {
            try {
                emailService.sendNarrativeEmail(
                        doctorEmail,
                        patient.getFullName(),
                        result.getAiRisk(),
                        result.getAiDiagnostic(),
                        doctorText
                );
                log.info("   📧 Email sent to doctor: {}", doctorEmail);
            } catch (Exception e) {
                log.error("   ❌ Email failed: {}", e.getMessage());
            }
        }

        // ── WhatsApp to patient (simple language) ──
        String phone = patient.getPhone();
        if (phone != null && !phone.isBlank()) {
            try {
                sendWhatsApp(phone,
                        "🧠 *MediCareAI — Your Lab Report*\n\n"
                        + patientText
                        + "\n\n_For questions, please contact your doctor._");
                log.info("   📱 WhatsApp sent to patient: {}", phone);
            } catch (Exception e) {
                log.error("   ❌ WhatsApp failed: {}", e.getMessage());
            }
        }
    }

    private void sendWhatsApp(String toPhone, String body) {
        Twilio.init(twilioSid, twilioToken);
        Message.creator(
                new PhoneNumber("whatsapp:" + toPhone),
                new PhoneNumber(twilioFrom),
                body
        ).create();
    }
}
