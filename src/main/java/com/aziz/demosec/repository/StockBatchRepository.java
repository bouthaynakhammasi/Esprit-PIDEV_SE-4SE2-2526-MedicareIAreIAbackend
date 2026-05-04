package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {
    List<StockBatch> findByPharmacyStockIdOrderByExpirationDateAsc(Long pharmacyStockId);
    List<StockBatch> findByExpirationDateBefore(LocalDate date);
    List<StockBatch> findByExpirationDateBetween(LocalDate start, LocalDate end);
    List<StockBatch> findByExpirationDateLessThanEqual(LocalDate date);

    @org.springframework.data.jpa.repository.Query("SELECT new com.aziz.demosec.dto.ExpirationRiskResponse(" +
            "b.id, p.name, b.batchNumber, b.quantity, b.expirationDate, " +
            "CASE WHEN b.expirationDate < :today THEN 'EXPIRED' " +
            "WHEN b.expirationDate <= :redZone THEN 'RED' " +
            "WHEN b.expirationDate <= :orangeZone THEN 'ORANGE' ELSE 'GREEN' END, " +
            "CASE WHEN b.expirationDate < :today THEN 'Destruction' " +
            "WHEN b.expirationDate <= :redZone THEN 'Evacuer (Retour Fournisseur / Don)' " +
            "WHEN b.expirationDate <= :orangeZone THEN 'Promotion / Vente rapide' ELSE 'Surveillance normale' END) " +
            "FROM StockBatch b JOIN b.pharmacyStock s JOIN s.product p " +
            "WHERE s.pharmacy.id = :pharmacyId AND b.quantity > 0 " +
            "ORDER BY b.expirationDate ASC")
    List<com.aziz.demosec.dto.ExpirationRiskResponse> getExpirationRiskDashboard(
            @org.springframework.data.repository.query.Param("pharmacyId") Long pharmacyId,
            @org.springframework.data.repository.query.Param("today") LocalDate today,
            @org.springframework.data.repository.query.Param("redZone") LocalDate redZone,
            @org.springframework.data.repository.query.Param("orangeZone") LocalDate orangeZone);

}