package com.aziz.demosec.Entities;


import com.aziz.demosec.domain.User;
import jakarta.persistence.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("Doctor")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Doctor extends User {
    private String specialty;
    @Column(name = "license_number", nullable = false)
    private String licenseNumber;
    private BigDecimal consultationFee;
    @Enumerated(EnumType.STRING)
    private ConsultationMode consultationMode;
    private String clinicAddress;

    @Column(columnDefinition = "TEXT")
    private String bio;
    
    @Column(name = "slot_duration")
    private Integer slotDuration = 30;

    private Integer patientCount = 0;
    private Integer yearsOfExperience;
    private Double rating = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id")
    private Clinic clinic;
}
