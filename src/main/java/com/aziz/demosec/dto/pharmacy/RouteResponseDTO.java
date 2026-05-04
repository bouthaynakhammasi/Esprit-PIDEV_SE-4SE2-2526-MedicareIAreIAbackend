package com.aziz.demosec.dto.pharmacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteResponseDTO {
    private double fromLat;
    private double fromLon;
    private double toLat;
    private double toLon;
    private double distanceKm;
    private double durationSec;
    private List<double[]> coordinates;
}
