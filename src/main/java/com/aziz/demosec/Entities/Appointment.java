package com.aziz.demosec.Entities;

import com.aziz.demosec.Entities.appointment.CalendarAvailability;
import com.aziz.demosec.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    @OneToOne
    @JoinColumn(name = "availability_id", nullable = false, unique = true)
    private CalendarAvailability availability;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    private AvailabilityMode mode;
    private String meetingLink;
    private String visitAddress;

}