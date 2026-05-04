package com.aziz.demosec.Entities;

import com.aziz.demosec.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_diary")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FoodDiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lifestyle_plan_id")
    private LifestylePlan lifestylePlan; // optional reference

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private HealthyRecipe recipe; // reference to catalog item

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String mealType; // BREAKFAST, LUNCH, DINNER, SNACK

    @Column(nullable = false)
    private String foodName;

    private Integer calories;

    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime loggedAt;
    @Column(columnDefinition = "LONGTEXT") // Vital for Base64 storage
    private String imageUrl;

}
