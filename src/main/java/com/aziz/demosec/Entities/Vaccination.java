package com.aziz.demosec.Entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "vaccinations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vaccination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baby_id", nullable = false)
    private BabyProfile baby;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = true)
    private String status;

    @Column(nullable = false)
    private String vaccineName;

    @Column(nullable = false)
    private LocalDate administeredDate;

    private String description;
}
