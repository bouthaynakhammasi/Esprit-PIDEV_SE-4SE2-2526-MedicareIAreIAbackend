package com.aziz.demosec.service;

import com.aziz.demosec.Entities.*;
import com.aziz.demosec.dto.homecare.AssignmentResultDTO;
import com.aziz.demosec.dto.homecare.ProviderScoreDTO;
import com.aziz.demosec.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class HomeCareAssignmentServiceImpl implements HomeCareAssignmentService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final ProviderAvailabilityRepository availabilityRepository;
    private final NotificationRepository notificationRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final EmailService emailService;

    private static final List<ServiceRequestStatus> ACTIVE_STATUSES =
            List.of(ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.IN_PROGRESS);

    // ──────────────────────────────────────────────────────────────
    // AUTO-ASSIGN : meilleur prestataire selon 3 critères
    // ──────────────────────────────────────────────────────────────
    @Override
    public AssignmentResultDTO autoAssign(Long requestId) {
        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceRequest not found: " + requestId));

        if (request.getStatus() != ServiceRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Seules les demandes PENDING peuvent être assignées. Statut actuel : " + request.getStatus());
        }

        Long serviceId = request.getService().getId();
        LocalDateTime requestedDateTime = request.getRequestedDateTime();
        int durationMinutes = request.getService().getDurationMinutes() != null
                ? request.getService().getDurationMinutes() : 60;

        // 1. Critère Spécialité : prestataires vérifiés ayant cette spécialité
        List<ServiceProvider> candidates = serviceProviderRepository.findVerifiedByServiceId(serviceId);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Aucun prestataire vérifié pour ce service : " + serviceId);
        }

        // 2. Critère Disponibilité + 3. Critère Charge de travail + Note
        ServiceProvider best = candidates.stream()
                .filter(p -> isAvailableForSlot(p.getId(), requestedDateTime, durationMinutes))
                .min(Comparator
                        .comparingInt((ServiceProvider p) -> countActiveWorkload(p.getId()))  // workload ASC
                        .thenComparingDouble(p -> -p.getAverageRating()))                      // rating DESC
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun prestataire disponible pour le créneau demandé."));

        // 3. Assigner et passer en ACCEPTED
        request.setAssignedProvider(best);
        request.setStatus(ServiceRequestStatus.ACCEPTED);
        request.setAssignedDateTime(LocalDateTime.now());
        serviceRequestRepository.save(request);

        // 4. Notifications patient + prestataire
        notifyAssignment(request, best);

        int workload = countActiveWorkload(best.getId());
        log.info("Demande #{} assignée au prestataire #{} (note={}, charge={})",
                requestId, best.getId(), best.getAverageRating(), workload);

        return AssignmentResultDTO.builder()
                .requestId(requestId)
                .assignedProviderId(best.getId())
                .providerName(best.getUser().getFullName())
                .providerRating(best.getAverageRating())
                .currentWorkload(workload)
                .assignedAt(LocalDateTime.now())
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    // RANK PROVIDERS : classement par score pour affichage admin
    // ──────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<ProviderScoreDTO> rankProviders(Long serviceId, LocalDateTime requestedDateTime) {
        List<ServiceProvider> candidates = serviceProviderRepository.findVerifiedByServiceId(serviceId);
        int duration = 60;

        return candidates.stream()
                .map(p -> {
                    boolean available = isAvailableForSlot(p.getId(), requestedDateTime, duration);
                    int workload = countActiveWorkload(p.getId());
                    double score = available ? computeScore(p.getAverageRating(), workload) : 0.0;

                    return ProviderScoreDTO.builder()
                            .providerId(p.getId())
                            .providerName(p.getUser().getFullName())
                            .rating(p.getAverageRating())
                            .workload(workload)
                            .available(available)
                            .score(score)
                            .build();
                })
                .sorted(Comparator.comparingDouble(ProviderScoreDTO::getScore).reversed())
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────────────────────────

    /**
     * Score = 60% charge de travail (inversée) + 40% note moyenne.
     * Plus le score est élevé, meilleur est le candidat.
     */
    private double computeScore(double rating, int workload) {
        double ratingScore  = (rating / 5.0) * 0.4;
        double workloadScore = (1.0 / (workload + 1)) * 0.6;
        return ratingScore + workloadScore;
    }

    /**
     * Compte les demandes actives (ACCEPTED + IN_PROGRESS) d'un prestataire.
     */
    private int countActiveWorkload(Long providerId) {
        return serviceRequestRepository.countByAssignedProvider_IdAndStatusIn(providerId, ACTIVE_STATUSES);
    }

    /**
     * Vérifie que le prestataire est disponible sur toute la durée du créneau
     * (vérification par tranches de 15 minutes).
     */
    private boolean isAvailableForSlot(Long providerId, LocalDateTime start, int durationMinutes) {
        LocalDateTime end = start.plusMinutes(durationMinutes);
        for (LocalDateTime t = start; t.isBefore(end); t = t.plusMinutes(15)) {
            if (!isAvailableAt(providerId, t)) return false;
        }
        return isAvailableAt(providerId, end.minusSeconds(1));
    }

    /**
     * Vérifie la disponibilité à un instant précis :
     * 1. Exceptions de date spécifique (priorité absolue)
     * 2. Règles hebdomadaires récurrentes (fallback)
     */
    private boolean isAvailableAt(Long providerId, LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();
        DayOfWeek dow = date.getDayOfWeek();

        // Exceptions de date spécifique (priorité absolue)
        List<ProviderAvailability> exceptions =
                availabilityRepository.findByProvider_IdAndSpecificDate(providerId, date);
        if (!exceptions.isEmpty()) {
            return exceptions.stream()
                    .anyMatch(a -> a.isAvailable()
                            && !time.isBefore(a.getStartTime())
                            && time.isBefore(a.getEndTime()));
        }

        // Règles hebdomadaires récurrentes
        List<ProviderAvailability> weekly =
                availabilityRepository.findByProvider_IdAndDayOfWeekAndSpecificDateIsNull(providerId, dow);
        return weekly.stream()
                .anyMatch(a -> a.isAvailable()
                        && !time.isBefore(a.getStartTime())
                        && time.isBefore(a.getEndTime()));
    }

    private void notifyAssignment(ServiceRequest request, ServiceProvider provider) {
        // Notifier le patient
        try {
            Notification patientNotif = Notification.builder()
                    .recipient(request.getPatient())
                    .type(NotificationType.HOMECARE_REQUEST_ACCEPTED)
                    .title("Demande de soin acceptée ✅")
                    .message("Votre demande #" + request.getId() + " a été assignée à "
                            + provider.getUser().getFullName() + ".")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            Notification saved = notificationRepository.save(patientNotif);
            webSocketNotificationService.notifyUser(request.getPatient().getId(), saved);
        } catch (Exception e) {
            log.warn("Notification patient échouée pour demande #{}: {}", request.getId(), e.getMessage());
        }

        // Notifier le prestataire
        try {
            Notification providerNotif = Notification.builder()
                    .recipient(provider.getUser())
                    .type(NotificationType.NEW_HOMECARE_REQUEST)
                    .title("Nouvelle demande assignée 🏥")
                    .message("Vous avez été assigné automatiquement à la demande #" + request.getId() + ".")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            Notification saved = notificationRepository.save(providerNotif);
            webSocketNotificationService.notifyUser(provider.getUser().getId(), saved);
        } catch (Exception e) {
            log.warn("Notification prestataire #{} échouée: {}", provider.getId(), e.getMessage());
        }

        // Email patient — confirmation Home Care
        try {
            String patientEmail = request.getPatient().getEmail();
            if (patientEmail != null) {
                emailService.sendHomeCareAssigned(
                        patientEmail,
                        request.getPatient().getFullName(),
                        request.getId(),
                        provider.getUser().getFullName(),
                        request.getService().getName(),
                        request.getRequestedDateTime()
                );
            }
        } catch (Exception e) {
            log.warn("Email home care assigné échoué pour demande #{}: {}", request.getId(), e.getMessage());
        }
    }
}
