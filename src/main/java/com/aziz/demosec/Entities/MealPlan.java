package com.aziz.demosec.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "meal_plan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lifestyle_plan_id", nullable = false)
    private LifestylePlan lifestylePlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private com.aziz.demosec.Entities.HealthyRecipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false)
    private String dayOfWeek; // MONDAY, TUESDAY, ...

    @Column(nullable = false)
    private String mealType; // BREAKFAST, LUNCH, DINNER, SNACK

    @Column(name = "week_number")
    private Integer weekNumber; // 1, 2, 3, 4

    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
