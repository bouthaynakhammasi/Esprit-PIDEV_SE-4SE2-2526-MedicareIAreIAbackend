package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.EmergencyAlert;
import com.aziz.demosec.Entities.EmergencyAlertStatus;
import com.aziz.demosec.Entities.EmergencySeverity;
import com.aziz.demosec.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {

    List<EmergencyAlert> findByStatus(EmergencyAlertStatus status);
    List<EmergencyAlert> findBySeverity(EmergencySeverity severity);
    List<EmergencyAlert> findByDeviceId(Long deviceId);
    List<EmergencyAlert> findByCanceledByPatient(Boolean canceled);

    /**
     * Fetches the patient linked to an alert using 2 explicit JOINs:
     *   JOIN 1 : EmergencyAlert → SmartDevice  (ea.device)
     *   JOIN 2 : SmartDevice    → User/Patient  (sd.patient)
     *
     * Equivalent SQL:
     *   SELECT u.*
     *   FROM emergency_alerts ea
     *   JOIN smart_devices    sd ON ea.device_id    = sd.id
     *   JOIN users             u  ON sd.patient_id  = u.id
     *   WHERE ea.id = :alertId
     */
    @Query("SELECT p FROM EmergencyAlert ea JOIN ea.device sd JOIN sd.patient p WHERE ea.id = :alertId")
    Optional<User> findPatientByAlertId(@Param("alertId") Long alertId);

    /**
     * Expired-alert auto-cancel query — 3 explicit JOINs:
     *   JOIN 1 : EmergencyAlert → SmartDevice          (ea.device)
     *   JOIN 2 : SmartDevice    → Patient               (sd.patient)
     *   JOIN 3 : EmergencyAlert → EmergencyIntervention (ea.intervention, LEFT JOIN)
     *
     * Returns alerts that:
     *  - are in a non-terminal pending status
     *  - were created before :cutoff (i.e. older than 5 minutes)
     *  - have NO intervention dispatched yet (ei.id IS NULL)
     */
    @Query("""
        SELECT ea FROM EmergencyAlert ea
        JOIN ea.device sd
        JOIN sd.patient p
        LEFT JOIN ea.intervention ei
        WHERE ea.status IN :statuses
        AND ea.createdAt < :cutoff
        AND ei.id IS NULL
    """)
    List<EmergencyAlert> findExpiredUnhandledAlerts(
        @Param("statuses") List<EmergencyAlertStatus> statuses,
        @Param("cutoff") LocalDateTime cutoff
    );
}
