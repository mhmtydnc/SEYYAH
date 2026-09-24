package com.seyyah.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OpenRouteService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouteService.class);

    private static final int YOL_ARAMA_YARICAPI_M = 5000;

    private final RestClient restClient;

    // Boot'un hazırladığı builder kullanılır: zaman aşımları spring.http.client.* ayarlarından gelir
    public OpenRouteService(
            RestClient.Builder builder,
            @Value("${ors.api.url}") String apiUrl,
            @Value("${ors.api.key}") String apiKey) {
        this.restClient = builder
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultStatusHandler(HttpStatusCode::isError, this::hatayiCevir)
                .build();
    }

    // Başlangıçtan bitişe araç rotası: WKT (koridor sorgusu için), koordinatlar, mesafe ve süre
    public RotaSonucu getRoute(double startLon, double startLat, double endLon, double endLat) {
        Map<String, Object> requestBody = Map.of(
                "coordinates", List.of(
                        List.of(startLon, startLat),
                        List.of(endLon, endLat)
                ),
                // Varsayılan 350 m: büyük ören yerlerinin (Efes) merkezi yola bu kadar yakın değil,
                // aramada gezi yerleri de hedef olarak seçilebildiği için en yakın yol 5 km'ye kadar aranır
                "radiuses", List.of(YOL_ARAMA_YARICAPI_M, YOL_ARAMA_YARICAPI_M)
        );

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri("/v2/directions/driving-car/geojson")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);
        } catch (ResourceAccessException e) {
            log.warn("ORS'a ulaşılamadı: {}", e.getMessage());
            throw new RotaServisiException(HttpStatus.GATEWAY_TIMEOUT, "Rota servisine ulaşılamadı", e);
        }

        return parseRouteResponse(response);
    }

    public String getRouteWkt(double startLon, double startLat, double endLon, double endLat) {
        return getRoute(startLon, startLat, endLon, endLat).wkt();
    }

    private void hatayiCevir(HttpRequest request, ClientHttpResponse response) throws IOException {
        int kod = response.getStatusCode().value();
        String govde = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        log.warn("ORS {} döndürdü: {}", kod, govde);

        throw switch (kod) {
            // ORS: 2009 rota bulunamadı, 2010 nokta yol ağına bağlanamadı
            case 404 -> new RotaServisiException(HttpStatus.NOT_FOUND,
                    "Bu iki nokta arasında rota bulunamadı");
            case 400 -> new RotaServisiException(HttpStatus.BAD_REQUEST,
                    "Rota isteği geçersiz (noktalar çok uzak ya da yol ağı dışında olabilir)");
            // Anahtar hatası istemcinin değil bizim sorunumuz
            case 401, 403 -> new RotaServisiException(HttpStatus.BAD_GATEWAY,
                    "Rota servisi yapılandırma hatası");
            case 429 -> new RotaServisiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Rota servisi kotası doldu, daha sonra tekrar deneyin");
            default -> new RotaServisiException(HttpStatus.BAD_GATEWAY,
                    "Rota servisi hata verdi");
        };
    }

    @SuppressWarnings("unchecked")
    private RotaSonucu parseRouteResponse(Map<String, Object> response) {
        List<Map<String, Object>> features = response == null ? null
                : (List<Map<String, Object>>) response.get("features");
        if (features == null || features.isEmpty()) {
            throw new RotaServisiException(HttpStatus.NOT_FOUND, "Bu iki nokta arasında rota bulunamadı");
        }

        Map<String, Object> feature = features.get(0);
        Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
        Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
        Map<String, Object> summary = properties == null ? null
                : (Map<String, Object>) properties.get("summary");

        // Number: JSON'da tam sayı gelen koordinat Integer olarak ayrıştırılır
        List<List<Number>> coords = (List<List<Number>>) geometry.get("coordinates");

        StringBuilder sb = new StringBuilder("SRID=4326;LINESTRING(");
        List<List<Double>> jsonCoords = new ArrayList<>();
        for (int i = 0; i < coords.size(); i++) {
            List<Number> pt = coords.get(i);
            double lon = pt.get(0).doubleValue();
            double lat = pt.get(1).doubleValue();
            sb.append(lon).append(" ").append(lat);
            if (i < coords.size() - 1) {
                sb.append(", ");
            }
            jsonCoords.add(List.of(lon, lat));
        }
        sb.append(")");

        // Aynı noktadan aynı noktaya rotada ORS özetinde mesafe/süre olmayabilir
        double distance = summary != null && summary.get("distance") instanceof Number n ? n.doubleValue() : 0.0;
        double duration = summary != null && summary.get("duration") instanceof Number n ? n.doubleValue() : 0.0;

        return new RotaSonucu(sb.toString(), jsonCoords, distance, duration);
    }
}
