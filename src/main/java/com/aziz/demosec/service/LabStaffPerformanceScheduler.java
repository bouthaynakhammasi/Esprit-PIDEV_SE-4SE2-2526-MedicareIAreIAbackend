package com.aziz.demosec.service;

import com.aziz.demosec.dto.LabStaffPerformanceDTO;
import com.aziz.demosec.repository.LabResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LabStaffPerformanceScheduler {

    private final LabResultRepository labResultRepository;
    private final EmailService         emailService;

    // Every Monday at 07:00
    @Scheduled(cron = "0 0 7 * * MON")
    public void sendDailyPerformanceReport() {

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 [SCHEDULER] LabStaff Performance — START");
        log.info("⏰ [SCHEDULER] Fired at : {}", LocalDateTime.now());

        // Last 7 days (full week)
        LocalDateTime from = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime to   = LocalDateTime.now();

        log.info("📅 [SCHEDULER] Period  : {} → {}", from, to);

        // ── Step 1 : query database ──
        log.info("🔍 [SCHEDULER] Step 1 — Querying database...");
        List<LabStaffPerformanceDTO> stats;
        Long totalAnalyses;
        Long urgentCount;

        try {
            stats         = labResultRepository.findStaffPerformance(from, to);
            totalAnalyses = labResultRepository.countAnalysesBetween(from, to);
            urgentCount   = labResultRepository.countUrgentBetween(from, to);
            log.info("✅ [SCHEDULER] DB query OK — {} technician(s) found", stats.size());
        } catch (Exception e) {
            log.error("❌ [SCHEDULER] DB query FAILED : {}", e.getMessage(), e);
            return;
        }

        // ── Step 2 : check data ──
        log.info("🔍 [SCHEDULER] Step 2 — Checking data...");
        log.info("📊 [SCHEDULER] Total analyses : {}", totalAnalyses);
        log.info("🚨 [SCHEDULER] Urgent cases   : {}", urgentCount);

        if (stats.isEmpty()) {
            log.warn("⚠️  [SCHEDULER] No data found for this period — email NOT sent");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return;
        }

        // ── Step 3 : log each technician ──
        log.info("🔍 [SCHEDULER] Step 3 — Technician details :");
        for (LabStaffPerformanceDTO s : stats) {
            log.info("   👤 {} | total={} | urgent={} | attention={} | surveillance={} | urgentRate={}%",
                    s.getTechnicianName(),
                    s.getTotalAnalyses(),
                    s.getUrgentCases(),
                    s.getAttentionCases(),
                    s.getSurveillanceCases(),
                    s.getUrgentRate());
        }

        // ── Step 4 : send email ──
        log.info("🔍 [SCHEDULER] Step 4 — Sending email to wassimzarai28@gmail.com ...");
        try {
            emailService.sendPerformanceReport(stats, totalAnalyses, urgentCount,
                    from.toLocalDate().toString());
            log.info("✅ [SCHEDULER] Email sent successfully !");
        } catch (Exception e) {
            log.error("❌ [SCHEDULER] Email send FAILED : {}", e.getMessage(), e);
        }

        log.info("🏁 [SCHEDULER] LabStaff Performance — END");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
