package com.aziz.demosec.service;

import com.aziz.demosec.Entities.AnomalyType;
import com.aziz.demosec.Entities.DailyHealthReport;
import com.aziz.demosec.Entities.LifestylePlan;
import com.aziz.demosec.Entities.Patient;
import com.aziz.demosec.Entities.PlanStatus;
import com.aziz.demosec.config.HealthThresholds;
import com.aziz.demosec.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyHealthReportService {

    private final LifestylePlanRepository lifestylePlanRepository;
    private final FoodDiaryRepository foodDiaryRepository;
    private final DailyHealthReportRepository reportRepository;
    private final HealthThresholds thresholds;

    @Transactional
    public DailyHealthReport generateReport(Patient patient, LocalDate date) {
        // Find active lifestyle plan for this patient
        List<LifestylePlan> activePlans = lifestylePlanRepository
                .findByGoal_Patient_IdAndStatus(patient.getId(), PlanStatus.ACTIVE);

        if (activePlans.isEmpty()) {
            log.debug("No active plan for patient {}", patient.getId());
            return null;
        }

        LifestylePlan plan = activePlans.get(0);

        // 1. Calculate actual calories from FoodDiary
        Integer actualCalories = foodDiaryRepository
                .sumCaloriesByPatientAndDate(patient.getId(), date);
        actualCalories = actualCalories != null ? actualCalories : 0;

        // 2. Get expected calories from LifestylePlan's MealPlan
        Double expectedCaloriesRaw = lifestylePlanRepository
                .sumExpectedCaloriesByPlanId(plan.getId());
        Integer expectedCalories = expectedCaloriesRaw != null ? expectedCaloriesRaw.intValue() : 0;

        // 3. Calculate calorie difference
        int calorieDiff = actualCalories - expectedCalories;

        // 4. Weight progress
        Double currentWeight = patient.getWeight();
        Double goalWeight = (plan.getGoal() != null && plan.getGoal().getTargetValue() != null)
                ? plan.getGoal().getTargetValue().doubleValue() : null;
        Double weightDiff = (currentWeight != null && goalWeight != null)
                ? currentWeight - goalWeight : null;

        // 5. Detect anomalies
        List<AnomalyType> anomalies = new ArrayList<>();
        boolean missedLog = false;

        if (actualCalories == 0) {
            anomalies.add(com.aziz.demosec.Entities.AnomalyType.MISSED_LOG);
            missedLog = true;
        }

        if (!missedLog && calorieDiff > thresholds.getOvereatingSurplus()) {
            anomalies.add(AnomalyType.OVEREATING);
        }

        if (!missedLog && calorieDiff < -thresholds.getUndereatingSurplus()) {
            anomalies.add(AnomalyType.UNDEREATING);
        }

        if (currentWeight != null && goalWeight != null) {
            boolean noProgress = checkNoWeightProgress(patient.getId(), thresholds.getNoProgressDaysWindow(), date);
            if (noProgress) {
                anomalies.add(AnomalyType.NO_WEIGHT_PROGRESS);
            }
        }

        // 6. Check if report already exists for this date, if so, update it
        Optional<DailyHealthReport> existing = reportRepository.findByPatientIdAndReportDate(patient.getId(), date);

        DailyHealthReport report = existing.orElse(new DailyHealthReport());
        report.setPatient(patient);
        report.setLifestylePlan(plan);
        report.setReportDate(date);
        report.setActualCalories(actualCalories);
        report.setExpectedCalories(expectedCalories);
        report.setCalorieDifference(calorieDiff);
        report.setCurrentWeight(currentWeight);
        report.setGoalWeight(goalWeight);
        report.setWeightDifference(weightDiff);
        report.setMissedLog(missedLog);
        report.setAnomalyDetected(!anomalies.isEmpty());
        report.setAnomalies(anomalies);

        return reportRepository.save(report);
    }

    private boolean checkNoWeightProgress(Long patientId, int daysWindow, LocalDate currentDate) {
        LocalDate from = currentDate.minusDays(daysWindow);
        List<DailyHealthReport> recentReports = reportRepository
                .findByPatientIdAndReportDateBetween(patientId, from, currentDate);

        if (recentReports.size() < 2) return false;

        Double firstWeight = recentReports.get(0).getCurrentWeight();
        Double lastWeight  = recentReports.get(recentReports.size() - 1).getCurrentWeight();

        if (firstWeight == null || lastWeight == null) return false;

        return Math.abs(lastWeight - firstWeight) < thresholds.getNoProgressWeightDelta();
    }
}