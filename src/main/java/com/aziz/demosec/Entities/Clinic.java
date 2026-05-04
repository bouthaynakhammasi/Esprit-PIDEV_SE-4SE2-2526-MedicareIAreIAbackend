package com.aziz.demosec.Entities;

import com.aziz.demosec.domain.User;
import jakarta.persistence.*;
import lombok.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@DiscriminatorValue("Clinic")
@Table(name = "clinic")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Clinic extends User {

    @Column(name = "name", nullable = false)
    private String name;

    private String address;
    private String phone;
    private Double latitude;
    private Double longitude;

    @Column(name = "has_emergency", nullable = false)
    private boolean hasEmergency;



    private String emergencyPhone;
    private boolean hasAmbulance;
    private String ambulancePhone;


    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean verified = false;

    // Aliases to avoid breaking API references using getName or getPhone implicitly
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setFullName(name);
    }
}
