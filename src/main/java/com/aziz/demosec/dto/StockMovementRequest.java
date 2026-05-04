package com.aziz.demosec.dto;

import com.aziz.demosec.Entities.StockMovementType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StockMovementRequest {

    @NotNull(message = "Pharmacy stock ID is required")
    @Positive(message = "Pharmacy stock ID must be positive")
    private Long pharmacyStockId;

    @NotNull(message = "Movement type is required")
    private StockMovementType movementType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;

    @Size(max = 255, message = "Reference must not exceed 255 characters")
    private String reference;
}