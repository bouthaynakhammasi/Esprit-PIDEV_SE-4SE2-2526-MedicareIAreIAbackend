package com.aziz.demosec.dto.donation;

/**
 * Projection résultat de la requête native avec 4 jointures :
 * donation_assignments → aid_requests → users (demandeur)
 *                      → donations    → users (donneur)
 */
public interface AssignmentEmailProjection {
    String getRequesterName();
    String getRequesterEmail();
    String getRequesterPhone();
    String getDonorName();
    String getDonorEmail();
    String getDonorPhone();
}
