package com.aziz.demosec.Entities;

import com.aziz.demosec.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("Patient")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Patient extends User {

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private BloodType bloodType;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String glucoseRate;
    private String allergies;
    private String diseases;
    private String chronicDiseases;
    private String drugAllergies;
    private String hereditaryDiseases;
    private Double height;
    private Double weight;
}