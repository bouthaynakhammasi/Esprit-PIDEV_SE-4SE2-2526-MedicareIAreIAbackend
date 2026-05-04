package com.aziz.demosec.service;

import com.aziz.demosec.Entities.LabRequestStatus;
import com.aziz.demosec.repository.LabRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component          // Spring détecte cette classe automatiquement
@RequiredArgsConstructor
@Slf4j
public class LabRequestScheduler {

    private final LabRequestRepository labRequestRepository;

    /**
     * BUSINESS LOGIC DU SCHEDULER :
     * Toute demande dont le statut est PENDING depuis plus de 48h
     * est automatiquement annulée (CANCELLED).
     *
     * fixedRate = 3600000 ms = 1 heure
     */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void cancelExpiredPendingRequests() {

        // Calcul de la date limite : maintenant - 48h
        LocalDateTime cutoff = LocalDateTime.now().minusHours(48);

        log.info("⏰ [SCHEDULER] Démarrage - Annulation des PENDING avant {}", cutoff);

        // Compte avant pour le log
        long countBefore = labRequestRepository
                .countByStatusAndRequestedAtBefore(LabRequestStatus.PENDING, cutoff);

        if (countBefore == 0) {
            log.info("✅ [SCHEDULER] Aucune demande expirée - Rien à faire");
            return;
        }

        // Mise à jour en base : PENDING → CANCELLED
        int updated = labRequestRepository.cancelExpiredRequests(
                cutoff,
                LabRequestStatus.PENDING,
                LabRequestStatus.CANCELLED
        );

        log.info("✅ [SCHEDULER] {} demande(s) PENDING annulée(s) automatiquement", updated);
    }
}