package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.PharmacyOrder;
import com.aziz.demosec.Entities.PharmacyOrderStatus;
import com.aziz.demosec.dto.pharmacy.OrderAgingDTO;
import com.aziz.demosec.dto.pharmacy.ProductSalesStatsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PharmacyOrderRepository extends JpaRepository<PharmacyOrder, Long> {

    List<PharmacyOrder> findByPatient_Id(Long patientId);

    List<PharmacyOrder> findByPharmacy_Id(Long pharmacyId);

    List<PharmacyOrder> findByStatus(PharmacyOrderStatus status);

    List<PharmacyOrder> findByPatient_IdAndStatus(Long patientId, PharmacyOrderStatus status);

    // Task 1 – Scheduler : commandes PENDING depuis plus de 48h
    List<PharmacyOrder> findByStatusAndCreatedAtBefore(PharmacyOrderStatus status, LocalDateTime dateTime);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PharmacyOrder o SET o.status = :status, o.updatedAt = :updatedAt WHERE o.id = :id")
    void updateOrderStatus(@Param("id") Long id, @Param("status") PharmacyOrderStatus status, @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE PharmacyOrder o SET o.status = :status, o.updatedAt = :updatedAt, o.pharmacistNote = :note WHERE o.id = :id")
    void updateOrderStatusWithNote(@Param("id") Long id, @Param("status") PharmacyOrderStatus status, @Param("note") String note, @Param("updatedAt") LocalDateTime updatedAt);


    @Query("SELECT new com.aziz.demosec.dto.pharmacy.ProductSalesStatsDTO(" +
           "    p.id, p.name, p.category," +
           "    SUM(CAST(i.quantity AS long))," +
           "    SUM(i.price)" +
           ") " +
           "FROM PharmacyOrder o " +
           "JOIN o.items i " +
           "JOIN i.product p " +
           "WHERE o.pharmacy.id = :pharmacyId " +
           "AND o.status = com.aziz.demosec.Entities.PharmacyOrderStatus.DELIVERED " +
           "GROUP BY p.id, p.name, p.category " +
           "ORDER BY SUM(CAST(i.quantity AS long)) DESC")
    List<ProductSalesStatsDTO> findProductSalesStatsByPharmacy(@Param("pharmacyId") Long pharmacyId);


    long countByStatus(PharmacyOrderStatus status);

    List<PharmacyOrder> findByPharmacy_NameContainingIgnoreCaseAndStatus(
            String pharmacyName, PharmacyOrderStatus status);

    // Advanced — JPQL multi-join: orders aging report (PENDING + REVIEWING)
    @Query("SELECT new com.aziz.demosec.dto.pharmacy.OrderAgingDTO(" +
           "    o.id, ph.name, u.fullName, o.status, o.createdAt" +
           ") " +
           "FROM PharmacyOrder o " +
           "JOIN o.pharmacy ph " +
           "JOIN o.patient u " +
           "WHERE o.status IN :statuses " +
           "ORDER BY o.createdAt ASC")
    List<OrderAgingDTO> findOrdersAging(@Param("statuses") List<PharmacyOrderStatus> statuses);
}
