package com.aziz.demosec.dto.pharmacy;

import com.aziz.demosec.Entities.DeliveryType;
import com.aziz.demosec.validation.ValidTunisianDeliveryAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidTunisianDeliveryAddress
public class PharmacyOrderRequestDTO {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Pharmacy ID is required")
    private Long pharmacyId;

    private Long prescriptionId;

    @NotNull(message = "Delivery address is required")
    @Size(max = 255, message = "Delivery address must not exceed 255 characters")
    private String deliveryAddress;


    private LocalDate scheduledDeliveryDate;

    @NotNull(message = "Prescription image URL is required")
    @Size(max = 255, message = "Prescription image URL must not exceed 255 characters")
    private String prescriptionImageUrl;

    @NotNull(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemRequestDTO> items;

    @NotNull(message = "Delivery type is required")
    private DeliveryType deliveryType;
}
