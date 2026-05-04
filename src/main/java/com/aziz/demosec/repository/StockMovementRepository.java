package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByPharmacyStockIdOrderByCreatedAtDesc(Long pharmacyStockId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(sm.quantity), 0L) FROM StockMovement sm WHERE sm.pharmacyStock.id = :stockId AND sm.movementType = com.aziz.demosec.Entities.StockMovementType.OUT AND sm.createdAt >= :startDate")
    Long getTotalOutQuantitySince(@org.springframework.data.repository.query.Param("stockId") Long stockId, @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate);
}