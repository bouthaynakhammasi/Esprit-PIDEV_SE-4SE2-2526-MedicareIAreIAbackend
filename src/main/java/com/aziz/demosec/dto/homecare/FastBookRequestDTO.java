package com.aziz.demosec.dto.homecare;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FastBookRequestDTO {
    @NotNull
    private Long serviceId;
    @NotNull
    private LocalDateTime requestedDateTime;
    @NotNull
    @Size(max = 500)
    private String address;
    @Size(max = 1000)
    private String patientNotes;
}
