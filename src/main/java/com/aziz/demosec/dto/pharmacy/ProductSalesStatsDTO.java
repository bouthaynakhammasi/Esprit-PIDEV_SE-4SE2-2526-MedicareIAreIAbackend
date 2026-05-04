package com.aziz.demosec.dto.pharmacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO retourné par la requête JPQL avec jointures
 * PharmacyOrder → OrderItem → Product.
 * Représente les statistiques de vente par produit sur les commandes DELIVERED.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSalesStatsDTO {

    private Long   productId;
    private String productName;
    private String productCategory;
    /** Somme des quantités vendues (OrderItem.quantity) sur toutes les commandes DELIVERED */
    private Long   totalQuantitySold;
    /** Chiffre d'affaires total (somme des OrderItem.price) */
    private BigDecimal totalRevenue;
}
