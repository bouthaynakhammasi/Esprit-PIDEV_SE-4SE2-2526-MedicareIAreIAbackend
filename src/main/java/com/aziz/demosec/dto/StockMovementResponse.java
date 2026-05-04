package com.aziz.demosec.dto;

import com.aziz.demosec.Entities.StockMovementType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StockMovementResponse {
    private Long id;
    private Long pharmacyStockId;
    private StockMovementType movementType;
    private Integer quantity;
    private String reference;
    private LocalDateTime createdAt;

    private Long performedById; // si tu veux l’ajouter plus tard
}