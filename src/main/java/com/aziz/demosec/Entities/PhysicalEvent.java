package com.aziz.demosec.Entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "physical_events")
@DiscriminatorValue("PHYSICAL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class PhysicalEvent extends MedicalEvent {

    private String venueName;
    private String address;
    private String city;
    private String postalCode;
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "venue_type")
    private VenueType venueType;

    private Integer capacity;
}