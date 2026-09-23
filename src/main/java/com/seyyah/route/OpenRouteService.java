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
import java.util.List;
import java.util.Map;

@Service
public class OpenRouteService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouteService.class);

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

    // Başlangıçtan bitişe araç rotasını "SRID=4326;LINESTRING(...)" olarak döndürür
    public String getRouteWkt(double startLon, double startLat, double endLon, double endLat) {
        Map<String, Object> requestBody = Map.of(
                "coordinates", List.of(
                        List.of(startLon, startLat),
                        List.of(endLon, endLat)
                )
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

        return extractWktFromGeoJson(response);
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
    private String extractWktFromGeoJson(Map<String, Object> response) {
        List<Map<String, Object>> features = response == null ? null
                : (List<Map<String, Object>>) response.get("features");
        if (features == null || features.isEmpty()) {
            throw new RotaServisiException(HttpStatus.NOT_FOUND, "Bu iki nokta arasında rota bulunamadı");
        }

        // Tek rota istendiği için ilk feature güzergahtır
        Map<String, Object> geometry = (Map<String, Object>) features.get(0).get("geometry");
        // Number: JSON'da tam sayı gelen koordinat Integer olarak ayrıştırılır
        List<List<Number>> coords = (List<List<Number>>) geometry.get("coordinates");

        StringBuilder sb = new StringBuilder("SRID=4326;LINESTRING(");
        for (int i = 0; i < coords.size(); i++) {
            List<Number> pt = coords.get(i);
            sb.append(pt.get(0)).append(" ").append(pt.get(1));
            if (i < coords.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
