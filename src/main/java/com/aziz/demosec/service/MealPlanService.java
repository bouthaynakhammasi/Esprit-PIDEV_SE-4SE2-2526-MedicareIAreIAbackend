package com.aziz.demosec.service;

import com.aziz.demosec.Entities.HealthyRecipe;
import com.aziz.demosec.Entities.LifestylePlan;
import com.aziz.demosec.dto.MealPlanDTO;
import com.aziz.demosec.Entities.MealPlan;
import com.aziz.demosec.Entities.Patient;
import com.aziz.demosec.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final LifestylePlanRepository lifestylePlanRepository;
    private final HealthyRecipeRepository recipeRepository;
    private final PatientRepository patientRepository;

    public MealPlanDTO create(MealPlanDTO dto) {
        LifestylePlan lifestylePlan = lifestylePlanRepository.findById(dto.getLifestylePlanId())
                .orElseThrow(() -> new RuntimeException("LifestylePlan not found"));

        HealthyRecipe recipe = recipeRepository.findById(dto.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        MealPlan plan = MealPlan.builder()
                .lifestylePlan(lifestylePlan)
                .recipe(recipe)
                .patient(patient)
                .dayOfWeek(dto.getDayOfWeek())
                .mealType(dto.getMealType())
                .weekNumber(dto.getWeekNumber())
                .notes(dto.getNotes())
                .build();

        return toDTO(mealPlanRepository.save(plan));
    }

    public List<MealPlanDTO> getByLifestylePlan(Long lifestylePlanId) {
        return mealPlanRepository.findByLifestylePlanId(lifestylePlanId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<MealPlanDTO> getByLifestylePlanAndDay(Long lifestylePlanId, String dayOfWeek) {
        return mealPlanRepository.findByLifestylePlanIdAndDayOfWeek(lifestylePlanId, dayOfWeek)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<MealPlanDTO> getByPatient(Long patientId) {
        return mealPlanRepository.findByPatientId(patientId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public void delete(Long id) {
        mealPlanRepository.deleteById(id);
    }

    @org.springframework.transaction.annotation.Transactional
    public List<MealPlanDTO> saveAll(Long lifestylePlanId, List<MealPlanDTO> dtos) {
        // Clear existing meals for this plan
        mealPlanRepository.deleteByLifestylePlanId(lifestylePlanId);
        mealPlanRepository.flush(); // Force delete to happen now

        // Save new ones
        List<MealPlanDTO> result = dtos.stream()
                .map(this::create)
                .collect(Collectors.toList());
        
        return result;
    }

    private MealPlanDTO toDTO(MealPlan m) {
        return MealPlanDTO.builder()
                .id(m.getId())
                .lifestylePlanId(m.getLifestylePlan().getId())
                .lifestylePlanTitle(m.getLifestylePlan().getTitle())
                .recipeId(m.getRecipe().getId())
                .recipeTitle(m.getRecipe().getTitle())
                .recipeCalories(m.getRecipe().getNutrients() != null && m.getRecipe().getNutrients().getCalories() != null 
                    ? m.getRecipe().getNutrients().getCalories().intValue() : 0)
                .patientId(m.getPatient().getId())
                .patientName(m.getPatient().getFullName()) 
                .dayOfWeek(m.getDayOfWeek())
                .mealType(m.getMealType())
                .weekNumber(m.getWeekNumber())
                .notes(m.getNotes())
                .build();
    }
}
