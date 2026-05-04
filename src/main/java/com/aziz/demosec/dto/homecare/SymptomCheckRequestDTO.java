package com.aziz.demosec.dto.homecare;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SymptomCheckRequestDTO {

    @NotBlank(message = "Symptoms description is required")
    @Size(min = 5, max = 1000, message = "Symptoms must be between 5 and 1000 characters")
    private String symptoms;
}
