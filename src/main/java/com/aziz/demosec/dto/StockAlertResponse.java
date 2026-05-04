package com.aziz.demosec.dto;

import com.aziz.demosec.Entities.StockAlertType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StockAlertResponse {
    private Long id;
    private Long pharmacyStockId;
    private StockAlertType alertType;
    private String message;
    private LocalDateTime createdAt;
    private boolean resolved;
    private LocalDateTime resolvedAt;
}