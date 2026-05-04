package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.Donation;
import com.aziz.demosec.Entities.DonationAssignment;
import com.aziz.demosec.Entities.DonationStatus;
import com.aziz.demosec.dto.donation.AssignmentEmailProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DonationAssignmentRepository extends JpaRepository<DonationAssignment, Long> {

    List<DonationAssignment> findByDonationId(Long donationId);
    List<DonationAssignment> findByAidRequestId(Long aidRequestId);

    /**
     * Keyword-based multi-table query — spans DonationAssignment → AidRequest → Patient
     * to find all donations linked to a specific patient through their aid requests,
     * filtered by donation status.
     */
    @Query("""
            SELECT da.donation FROM DonationAssignment da
            WHERE da.aidRequest.patient.id = :patientId
              AND da.donation.status = :status
            """)
    List<Donation> findDonationsByPatientIdAndStatus(
            @Param("patientId") Long patientId,
            @Param("status") DonationStatus status);

    @Query(value = """
            SELECT
                u_requester.full_name  AS requester_name,
                u_requester.email      AS requester_email,
                u_requester.phone      AS requester_phone,
                u_donor.full_name      AS donor_name,
                u_donor.email          AS donor_email,
                u_donor.phone          AS donor_phone
            FROM donation_assignments da
            INNER JOIN aid_requests   ar ON da.aid_request_id = ar.id
            INNER JOIN donations       d ON da.donation_id    = d.id
            INNER JOIN users  u_requester ON ar.patient_id   = u_requester.id
            INNER JOIN users      u_donor ON d.creator_id    = u_donor.id
            WHERE da.id = :assignmentId
            """, nativeQuery = true)
    Optional<AssignmentEmailProjection> findEmailDataByAssignmentId(@Param("assignmentId") Long assignmentId);
}
