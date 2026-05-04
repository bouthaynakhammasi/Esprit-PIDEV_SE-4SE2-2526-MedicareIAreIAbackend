package com.aziz.demosec.Entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clinic")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clinic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    private String address;

    private Double latitude;
    private Double longitude;
    private String phone;



    @Column(name = "has_emergency", nullable = false)
    private boolean hasEmergency;

    @Column(name = "has_ambulance", nullable = false)
    private boolean hasAmbulance;
    private String emergencyPhone;
    private String ambulancePhone;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean verified = false;

    @Column(name = "user_id", unique = true)
    private Long userId;
}