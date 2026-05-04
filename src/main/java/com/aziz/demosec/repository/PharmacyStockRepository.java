package com.aziz.demosec.repository;

import com.aziz.demosec.Entities.PharmacyStock;
import org.springframework.data.jpa.repository.JpaRepository;
import com.aziz.demosec.dto.StockSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PharmacyStockRepository extends JpaRepository<PharmacyStock, Long> {
    Optional<PharmacyStock> findByPharmacyIdAndProductId(Long pharmacyId, Long productId);
    List<PharmacyStock> findByPharmacyId(Long pharmacyId);

    @Query("SELECT new com.aziz.demosec.dto.StockSummaryResponse(" +
            "ph.name, p.name, ps.totalQuantity, COUNT(sb), MIN(sb.expirationDate)) " +
            "FROM PharmacyStock ps " +
            "JOIN ps.pharmacy ph " +
            "JOIN ps.product p " +
            "LEFT JOIN StockBatch sb ON sb.pharmacyStock = ps " +
            "GROUP BY ph.name, p.name, ps.totalQuantity")
    List<StockSummaryResponse> getStockSummary();

    @Query("SELECT ps FROM PharmacyStock ps " +
            "JOIN ps.product p " +
            "JOIN ps.pharmacy ph " +
            "WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(ph.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<PharmacyStock> searchProducts(@Param("keyword") String keyword, Pageable pageable);
}
