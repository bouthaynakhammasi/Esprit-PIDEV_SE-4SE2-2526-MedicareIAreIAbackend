package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.Donation;
import com.aziz.demosec.Entities.DonationStatus;
import com.aziz.demosec.Entities.DonationType;
import com.aziz.demosec.dto.donation.TopDonorDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByStatus(DonationStatus status);
    List<Donation> findByType(DonationType type);

    @Query("""
            SELECT d FROM Donation d
            WHERE d.type = com.aziz.demosec.Entities.DonationType.MEDICAMENT
              AND d.status = com.aziz.demosec.Entities.DonationStatus.AVAILABLE
            """)
    List<Donation> findAvailableMedicamentDonations();

    /**
     * JPQL with join — returns AVAILABLE donations of the given category joined
     * with the creator User table, ensuring only donations with a valid donor
     * account are returned. Ordered most-recent first.
     */
    @Query("""
            SELECT d FROM Donation d, User u
            WHERE d.creatorId = u.id
              AND d.status = com.aziz.demosec.Entities.DonationStatus.AVAILABLE
              AND d.categorie = :category
            ORDER BY d.createdAt DESC
            """)
    List<Donation> findAvailableDonationsByCategory(@Param("category") String category);

    /**
     * JPQL with joins — ranks donors by how many times their donations were
     * actually assigned. Traversal: DonationAssignment → Donation → User (creator).
     * Returns a list of TopDonorDTO sorted by assignment count descending.
     */
    @Query("""
            SELECT new com.aziz.demosec.dto.donation.TopDonorDTO(
                u.id,
                u.fullName,
                u.profileImage,
                COUNT(da.id)
            )
            FROM DonationAssignment da, Donation d, User u
            WHERE da.donation.id = d.id
              AND d.creatorId = u.id
            GROUP BY u.id, u.fullName, u.profileImage
            ORDER BY COUNT(da.id) DESC
            """)
    List<TopDonorDTO> findTopDonorsByAssignmentCount();

    /**
     * Used by the nightly scheduler: bulk-update AVAILABLE donations created
     * before the given cutoff to EXPIRED in a single UPDATE statement.
     */
    @Modifying
    @Query("""
            UPDATE Donation d
            SET d.status = com.aziz.demosec.Entities.DonationStatus.EXPIRED
            WHERE d.status = com.aziz.demosec.Entities.DonationStatus.AVAILABLE
              AND d.createdAt < :cutoff
            """)
    int expireOldDonations(@Param("cutoff") LocalDateTime cutoff);
}
