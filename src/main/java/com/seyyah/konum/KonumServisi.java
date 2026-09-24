package com.seyyah.konum;

import com.seyyah.route.RotaServisiException;
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
public class KonumServisi {

    private static final Logger log = LoggerFactory.getLogger(KonumServisi.class);

    private final RestClient restClient;

    public KonumServisi(
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

    @SuppressWarnings("unchecked")
    public List<KonumSonucu> ara(String q, int size) {
        Map<String, Object> response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/geocode/autocomplete")
                            .queryParam("text", q)
                            .queryParam("size", size)
                            .queryParam("boundary.country", "TR")
                            .build())
                    .retrieve()
                    .body(Map.class);
        } catch (ResourceAccessException e) {
            log.warn("ORS'a ulaşılamadı: {}", e.getMessage());
            throw new RotaServisiException(HttpStatus.GATEWAY_TIMEOUT, "Konum servisine ulaşılamadı", e);
        }

        if (response == null) {
            return List.of();
        }

        List<Map<String, Object>> features = (List<Map<String, Object>>) response.get("features");
        if (features == null || features.isEmpty()) {
            return List.of();
        }

        List<KonumSonucu> sonuclar = new ArrayList<>();
        for (Map<String, Object> feature : features) {
            Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
            Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");

            if (properties == null || geometry == null) continue;

            String ad = (String) properties.get("name");
            String etiket = (String) properties.get("label");
            List<Number> coords = (List<Number>) geometry.get("coordinates");

            if (ad != null && coords != null && coords.size() >= 2) {
                sonuclar.add(new KonumSonucu(ad, etiket, coords.get(1).doubleValue(), coords.get(0).doubleValue()));
            }
        }

        return sonuclar;
    }

    private void hatayiCevir(HttpRequest request, ClientHttpResponse response) throws IOException {
        int kod = response.getStatusCode().value();
        String govde = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        log.warn("ORS geocode {} döndürdü: {}", kod, govde);

        throw switch (kod) {
            case 401, 403 -> new RotaServisiException(HttpStatus.BAD_GATEWAY,
                    "Konum servisi yapılandırma hatası");
            case 429 -> new RotaServisiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Konum servisi kotası doldu, daha sonra tekrar deneyin");
            default -> new RotaServisiException(HttpStatus.BAD_GATEWAY,
                    "Konum servisi hata verdi");
        };
    }
}
