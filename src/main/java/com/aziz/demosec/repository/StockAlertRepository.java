package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.StockAlert;
import com.aziz.demosec.Entities.StockAlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    List<StockAlert> findByResolvedFalseOrderByCreatedAtDesc();
    List<StockAlert> findByPharmacyStockIdAndResolvedFalse(Long pharmacyStockId);
    boolean existsByPharmacyStockIdAndAlertTypeAndResolvedFalse(Long pharmacyStockId, StockAlertType type);
}