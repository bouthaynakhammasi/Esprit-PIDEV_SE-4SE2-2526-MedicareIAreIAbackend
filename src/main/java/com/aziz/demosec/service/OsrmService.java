package com.aziz.demosec.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aziz.demosec.dto.pharmacy.RouteResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OsrmService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GeocodingService geocodingService;

    public double getDistanceKm(String fromAddress, String toAddress) {
        try {
            double[] from = geocodingService.getCoordinates(fromAddress);
            double[] to   = geocodingService.getCoordinates(toAddress);
            return getDistanceKmByCoords(from[0], from[1], to[0], to[1]);
        } catch (Exception e) {
            System.err.println("OSRM distance failed: " + e.getMessage() + " — fallback 5km");
            return 5.0;
        }
    }

    public double getDistanceKmByCoords(double fromLat, double fromLon, double toLat, double toLon) {
        try {
            double[] to = (toLat == 0 && toLon == 0)
                    ? geocodingService.getCoordinates("Tunis")
                    : new double[]{toLat, toLon};

            String url = String.format(Locale.US,
                "http://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=false",
                fromLon, fromLat, to[1], to[0]
            );

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            double distanceMeters = root.path("routes").get(0).path("distance").asDouble();
            return distanceMeters / 1000.0;

        } catch (Exception e) {
            System.err.println("OSRM coords failed: " + e.getMessage() + " — fallback 5km");
            return 5.0;
        }
    }

    public RouteResponseDTO getRoute(String fromAddress, String toAddress) {
        try {
            double[] from = geocodingService.getCoordinates(fromAddress);
            double[] to   = geocodingService.getCoordinates(toAddress);
            return getRouteByCoords(from[0], from[1], to[0], to[1]);
        } catch (Exception e) {
            System.err.println("OSRM route failed: " + e.getMessage());
            return null;
        }
    }

    public RouteResponseDTO getRouteByCoords(double fromLat, double fromLon, double toLat, double toLon) {
        try {
            double actualToLat = (toLat != 0) ? toLat : 36.8065;
            double actualToLon = (toLon != 0) ? toLon : 10.1815;

            String url = String.format(Locale.US,
                "http://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                fromLon, fromLat, actualToLon, actualToLat
            );

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode route = root.path("routes").get(0);

            double distanceKm = route.path("distance").asDouble() / 1000.0;
            double durationSec = route.path("duration").asDouble();

            // Extraire les coordonnées de la géométrie GeoJSON
            List<double[]> coordinates = new ArrayList<>();
            JsonNode coords = route.path("geometry").path("coordinates");
            for (JsonNode coord : coords) {
                coordinates.add(new double[]{coord.get(1).asDouble(), coord.get(0).asDouble()});
            }

            return RouteResponseDTO.builder()
                    .fromLat(fromLat).fromLon(fromLon)
                    .toLat(actualToLat).toLon(actualToLon)
                    .distanceKm(distanceKm)
                    .durationSec(durationSec)
                    .coordinates(coordinates)
                    .build();

        } catch (Exception e) {
            System.err.println("OSRM route failed: " + e.getMessage());
            return null;
        }
    }
}
