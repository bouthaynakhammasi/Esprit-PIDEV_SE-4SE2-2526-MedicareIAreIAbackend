package com.aziz.demosec.Entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "laboratories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Laboratory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;
    private String phone;
    private boolean active = true;
    private String email;
    private String openingHours;
    private String specializations; // ✅ String au lieu de List<String>

    @OneToMany(mappedBy = "laboratory", cascade = CascadeType.ALL)
    private List<LaboratoryStaff> staffMembers;

    @OneToMany(mappedBy = "laboratory", cascade = CascadeType.ALL)
    private List<LabTest> labTests;
}
