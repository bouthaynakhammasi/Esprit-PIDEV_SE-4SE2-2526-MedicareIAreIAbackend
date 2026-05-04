package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.ServiceRequest;
import com.aziz.demosec.Entities.ServiceRequestStatus;
import com.aziz.demosec.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByPatientOrderByCreatedAtDesc(User patient);
    List<ServiceRequest> findByAssignedProvider_IdOrderByCreatedAtDesc(Long providerId);
    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(ServiceRequestStatus status);
    List<ServiceRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(ServiceRequestStatus status);

    int countByAssignedProvider_IdAndStatusIn(Long providerId, List<ServiceRequestStatus> statuses);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE ServiceRequest r SET r.status = :status WHERE r.id = :id")
    void updateStatus(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("status") ServiceRequestStatus status);
}
