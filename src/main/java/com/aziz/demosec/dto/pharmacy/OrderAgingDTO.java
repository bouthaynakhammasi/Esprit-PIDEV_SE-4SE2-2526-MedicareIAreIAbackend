package com.aziz.demosec.dto.pharmacy;

import com.aziz.demosec.Entities.PharmacyOrderStatus;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
public class OrderAgingDTO {

    private final Long orderId;
    private final String pharmacyName;
    private final String patientName;
    private final String status;
    private final LocalDateTime createdAt;
    private final long hoursWaiting;
    private final String urgencyLevel;  // LOW / MEDIUM / HIGH / CRITICAL

    public OrderAgingDTO(Long orderId, String pharmacyName,
                         String patientFullName,
                         PharmacyOrderStatus status, LocalDateTime createdAt) {
        this.orderId      = orderId;
        this.pharmacyName = pharmacyName;
        this.patientName  = patientFullName;
        this.status       = status.name();
        this.createdAt    = createdAt;
        this.hoursWaiting = Duration.between(createdAt, LocalDateTime.now()).toHours();
        this.urgencyLevel = computeUrgency(this.hoursWaiting);
    }

    private static String computeUrgency(long hours) {
        if (hours < 1)  return "LOW";
        if (hours < 6)  return "MEDIUM";
        if (hours < 24) return "HIGH";
        return "CRITICAL";
    }
}
