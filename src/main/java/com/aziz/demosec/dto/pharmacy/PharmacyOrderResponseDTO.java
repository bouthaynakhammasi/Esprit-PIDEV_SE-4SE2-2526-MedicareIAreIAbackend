package com.aziz.demosec.dto.pharmacy;

import com.aziz.demosec.Entities.DeliveryType;
import com.aziz.demosec.Entities.PharmacyOrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyOrderResponseDTO {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long pharmacyId;
    private String pharmacyName;
    private String pharmacyAddress;
    private Long prescriptionId;
    private PharmacyOrderStatus status;
    private BigDecimal totalPrice;
    private String deliveryAddress;
    private LocalDate scheduledDeliveryDate;
    private DeliveryType deliveryType;
    private String pharmacistNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String prescriptionImageUrl;
    private Integer estimatedDeliveryMin;
    private List<OrderItemResponseDTO> items;
    private List<OrderTrackingResponseDTO> trackingHistory;
}
