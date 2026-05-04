package com.aziz.demosec.Entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.aziz.demosec.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "pharmacy_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PharmacyOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PharmacyOrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    private String deliveryAddress;

    private LocalDate scheduledDeliveryDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 1000)
    private String pharmacistNote;

    private String prescriptionImageUrl;

    @Enumerated(EnumType.STRING)
    private DeliveryType deliveryType;

    private Integer estimatedDeliveryMin;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderTracking> trackingHistory = new ArrayList<>();
}
