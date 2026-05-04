package com.aziz.demosec.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Fallback coordinates for major Tunisian cities
    private static final Map<String, double[]> CITY_COORDS = Map.ofEntries(
        Map.entry("tunis",      new double[]{36.8065, 10.1815}),
        Map.entry("sfax",       new double[]{34.7406, 10.7603}),
        Map.entry("sousse",     new double[]{35.8288, 10.6405}),
        Map.entry("kairouan",   new double[]{35.6781, 10.0994}),
        Map.entry("bizerte",    new double[]{37.2744, 9.8739}),
        Map.entry("gabes",      new double[]{33.8815, 10.0982}),
        Map.entry("ariana",     new double[]{36.8625, 10.1956}),
        Map.entry("gafsa",      new double[]{34.4250, 8.7842}),
        Map.entry("monastir",   new double[]{35.7643, 10.8113}),
        Map.entry("ben arous",  new double[]{36.7533, 10.2281}),
        Map.entry("kasserine",  new double[]{35.1671, 8.8365}),
        Map.entry("medenine",   new double[]{33.3549, 10.5055}),
        Map.entry("nabeul",     new double[]{36.4513, 10.7357}),
        Map.entry("beja",       new double[]{36.7256, 9.1817}),
        Map.entry("jendouba",   new double[]{36.5011, 8.7757}),
        Map.entry("mahdia",     new double[]{35.5047, 11.0622}),
        Map.entry("siliana",    new double[]{36.0840, 9.3708}),
        Map.entry("zaghouan",   new double[]{36.4029, 10.1430}),
        Map.entry("tozeur",     new double[]{33.9197, 8.1335}),
        Map.entry("kebili",     new double[]{33.7048, 8.9701}),
        Map.entry("tataouine",  new double[]{32.9211, 10.4518}),
        Map.entry("hammamet",   new double[]{36.4000, 10.6167}),
        Map.entry("djerba",     new double[]{33.8076, 10.8451})
    );

    public double[] getCoordinates(String address) {
        if (address == null || address.isBlank()) {
            return new double[]{36.8065, 10.1815};
        }

        // Attempt 1: full address + ", Tunisia"
        String queryWithCountry = address.toLowerCase().contains("tunisia") || address.toLowerCase().contains("tunisie")
                ? address : address + ", Tunisia";
        double[] result = nominatimSearch(queryWithCountry);
        if (result != null) {
            System.out.println("[Geocoding] ✅ Found by full address: " + address + " → " + result[0] + ", " + result[1]);
            return result;
        }

        // Attempt 2: city name only
        String city = extractCity(address);
        result = nominatimSearch(city + ", Tunisia");
        if (result != null) {
            System.out.println("[Geocoding] ✅ Found by city name: " + city + " → " + result[0] + ", " + result[1]);
            return result;
        }

        // Attempt 3: known city coordinate map
        double[] cityFallback = CITY_COORDS.get(city.toLowerCase());
        if (cityFallback != null) {
            System.out.println("[Geocoding] ⚠️ Using city map fallback for: " + city + " → " + cityFallback[0] + ", " + cityFallback[1]);
            return cityFallback;
        }

        System.err.println("[Geocoding] ❌ All geocoding attempts failed for: " + address + " — defaulting to Tunis");
        return new double[]{36.8065, 10.1815};
    }

    private double[] nominatimSearch(String query) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://nominatim.openstreetmap.org/search")
                    .queryParam("q", query)
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root.isArray() && root.size() > 0) {
                double lat = root.get(0).get("lat").asDouble();
                double lon = root.get(0).get("lon").asDouble();
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            System.err.println("[Geocoding] Nominatim error for query '" + query + "': " + e.getMessage());
        }
        return null;
    }

    public String extractCity(String address) {
        if (address == null || address.isBlank()) return "Tunis";
        String[] parts = address.split(",");
        // Last part is usually the city for "Street, City" format
        String last = parts[parts.length - 1].trim();
        // Filter out "Tunisia"/"Tunisie"
        if (last.equalsIgnoreCase("Tunisia") || last.equalsIgnoreCase("Tunisie")) {
            if (parts.length >= 2) return parts[parts.length - 2].trim();
        }
        if (parts.length >= 2) return parts[parts.length - 1].trim();
        return parts[0].trim();
    }
}
