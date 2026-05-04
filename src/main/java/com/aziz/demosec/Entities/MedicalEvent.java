package com.aziz.demosec.Entities;

import com.aziz.demosec.domain.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_events")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "event_kind")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class MedicalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;
    private String description;

    @Column(nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedicalEventType eventType;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl;

    private Integer capacity;

    private Double ticketPrice;
}