package com.aziz.demosec.Entities;

import com.aziz.demosec.domain.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Nutritionist extends User {

    @Column(name = "license_number", unique = true)
    private String licenseNumber;
    private String specialties;
    @Column(columnDefinition = "TEXT")
    private String bio;
    private Integer yearsOfExperience;
    private BigDecimal consultationFee;
    @Enumerated(EnumType.STRING)
    private ConsultationMode consultationMode;

}
