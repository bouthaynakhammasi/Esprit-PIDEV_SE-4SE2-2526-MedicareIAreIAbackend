package com.aziz.demosec.service;

import com.aziz.demosec.Entities.HealthyRecipe;
import com.aziz.demosec.Entities.LifestylePlan;
import com.aziz.demosec.dto.GroceryItemDTO;
import com.aziz.demosec.Entities.GroceryItem;
import com.aziz.demosec.Entities.Patient;
import com.aziz.demosec.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroceryListService {

    private final GroceryItemRepository groceryRepository;
    private final MealPlanRepository mealPlanRepository;
    private final LifestylePlanRepository lifestylePlanRepository;
    private final PatientRepository patientRepository;

    @Transactional
    public List<GroceryItemDTO> generateFromLifestylePlan(Long lifestylePlanId, Long patientId) {
        LifestylePlan lifestylePlan = lifestylePlanRepository.findById(lifestylePlanId)
                .orElseThrow(() -> new RuntimeException("LifestylePlan not found"));

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // Clear previous grocery list for this plan
        groceryRepository.deleteByLifestylePlanId(lifestylePlanId);

        List<HealthyRecipe> recipes = mealPlanRepository.findRecipesByLifestylePlanId(lifestylePlanId);

        List<GroceryItem> items = new ArrayList<>();
        for (HealthyRecipe recipe : recipes) {
            String[] ingredients = recipe.getIngredients().split(",");
            for (String ingredient : ingredients) {
                items.add(GroceryItem.builder()
                        .patient(patient)
                        .lifestylePlan(lifestylePlan)
                        .itemName(ingredient.trim())
                        .purchased(false)
                        .sourceRecipe(recipe.getTitle())
                        .build());
            }
        }

        return groceryRepository.saveAll(items)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<GroceryItemDTO> getByLifestylePlan(Long lifestylePlanId) {
        return groceryRepository.findByLifestylePlanId(lifestylePlanId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<GroceryItemDTO> getPendingItems(Long lifestylePlanId) {
        return groceryRepository.findByLifestylePlanIdAndPurchased(lifestylePlanId, false)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public GroceryItemDTO markPurchased(Long itemId) {
        GroceryItem item = groceryRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        item.setPurchased(true);
        return toDTO(groceryRepository.save(item));
    }

    @Transactional
    public void clearList(Long lifestylePlanId) {
        groceryRepository.deleteByLifestylePlanId(lifestylePlanId);
    }

    private GroceryItemDTO toDTO(GroceryItem g) {
        return GroceryItemDTO.builder()
                .id(g.getId())
                .patientId(g.getPatient().getId())
                .lifestylePlanId(g.getLifestylePlan().getId())
                .itemName(g.getItemName())
                .quantity(g.getQuantity())
                .unit(g.getUnit())
                .purchased(g.isPurchased())
                .sourceRecipe(g.getSourceRecipe())
                .createdAt(g.getCreatedAt())
                .build();
    }
}
