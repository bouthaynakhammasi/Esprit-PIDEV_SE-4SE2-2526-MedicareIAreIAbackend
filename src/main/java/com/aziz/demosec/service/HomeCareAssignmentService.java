package com.aziz.demosec.service;

import com.aziz.demosec.dto.homecare.AssignmentResultDTO;
import com.aziz.demosec.dto.homecare.ProviderScoreDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface HomeCareAssignmentService {

    /**
     * Auto-assigne le meilleur prestataire disponible à une demande PENDING.
     * Critères : spécialité, disponibilité, charge de travail, note moyenne.
     */
    AssignmentResultDTO autoAssign(Long requestId);

    /**
     * Classe tous les prestataires vérifiés d'un service par score d'adéquation.
     * Utile pour affichage admin avant assignation manuelle.
     */
    List<ProviderScoreDTO> rankProviders(Long serviceId, LocalDateTime requestedDateTime);
}
