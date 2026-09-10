package com.seyyah.route;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OpenRouteService {

    private final RestClient restClient;

    public OpenRouteService(
            @Value("${ors.api.url}") String apiUrl,
            @Value("${ors.api.key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // Tek ana rota için WKT listesi döndüren metot (şehirlerarası mesafeler için)
    public List<String> getRouteWkt(double startLon, double startLat, double endLon, double endLat) {
        // alternative_routes parametresi kaldırıldı, sadece coordinates gönderiliyor
        Map<String, Object> requestBody = Map.of(
                "coordinates", List.of(
                        List.of(startLon, startLat),
                        List.of(endLon, endLat)
                )
        );

        Map<String, Object> response = restClient.post()
                .uri("/v2/directions/driving-car/geojson")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        return extractWktFromGeoJson(response);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractWktFromGeoJson(Map<String, Object> response) {
        List<String> wktList = new ArrayList<>();
        if (response == null || !response.containsKey("features")) {
            return wktList;
        }

        List<Map<String, Object>> features = (List<Map<String, Object>>) response.get("features");
        if (!features.isEmpty()) {
            // Tek rota döndüğü için ilk feature (güzergah) alınır
            Map<String, Object> feature = features.get(0);
            Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
            List<List<Double>> coords = (List<List<Double>>) geometry.get("coordinates");

            StringBuilder sb = new StringBuilder("SRID=4326;LINESTRING(");
            for (int i = 0; i < coords.size(); i++) {
                List<Double> pt = coords.get(i);
                sb.append(pt.get(0)).append(" ").append(pt.get(1));
                if (i < coords.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            wktList.add(sb.toString());
        }
        return wktList;
    }
}