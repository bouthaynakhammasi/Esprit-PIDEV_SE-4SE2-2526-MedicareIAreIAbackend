package com.aziz.demosec.Entities;

import com.aziz.demosec.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@DiscriminatorValue("Pharmacist")
@EqualsAndHashCode(callSuper = true)
public class Pharmacist extends User {

    @ManyToOne
    @JoinColumn(name = "pharmacy_id")
    private Pharmacy pharmacy;

    @Column(name = "pharmacy_name")
    private String pharmacyName;

    @Column(name = "pharmacy_address")
    private String pharmacyAddress;

    @Column(name = "pharmacy_phone")
    private String pharmacyPhone;

    @Column(name = "pharmacy_email")
    private String pharmacyEmail;

    @Column(name = "pharmacy_latitude")
    private Float pharmacyLatitude;

    @Column(name = "pharmacy_longitude")
    private Float pharmacyLongitude;

    @Column(name = "pharmacy_setup_completed", nullable = false)
    private boolean pharmacySetupCompleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PharmacistStatus status = PharmacistStatus.PENDING;

    @Column(name = "diploma_document", columnDefinition = "LONGTEXT")
    private String diplomaDocument;
}