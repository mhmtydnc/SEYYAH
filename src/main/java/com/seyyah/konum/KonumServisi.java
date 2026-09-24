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
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KonumServisi {

    private static final Logger log = LoggerFactory.getLogger(KonumServisi.class);

    private final RestClient restClient;
    private final JdbcClient jdbcClient;

    public KonumServisi(
            RestClient.Builder builder,
            JdbcClient jdbcClient,
            @Value("${ors.api.url}") String apiUrl,
            @Value("${ors.api.key}") String apiKey) {
        this.jdbcClient = jdbcClient;
        this.restClient = builder
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultStatusHandler(HttpStatusCode::isError, this::hatayiCevir)
                .build();
    }

    @SuppressWarnings("unchecked")
    public List<KonumSonucu> ara(String q, int size) {
        List<KonumSonucu> dbSonuclar = jdbcClient.sql("""
                WITH q_yerlesim AS (
                    SELECT
                        ad,
                        CASE
                            WHEN il IS NOT NULL AND il != ad THEN ad || ', ' || il
                            WHEN tur = 'city' THEN ad || ' (şehir)'
                            WHEN tur = 'town' THEN ad || ' (ilçe/kasaba)'
                            WHEN tur = 'suburb' THEN ad || ' (mahalle)'
                            WHEN tur = 'village' THEN ad || ' (köy)'
                            ELSE ad || ' (' || tur || ')'
                        END AS etiket,
                        ST_Y(konum::geometry) AS enlem,
                        ST_X(konum::geometry) AS boylam,
                        onem,
                        nufus,
                        1 AS tip_sirasi,
                        CASE WHEN lower(f_unaccent(ad)) LIKE lower(f_unaccent(:q)) || '%' THEN 1 ELSE 0 END AS onek_eslesme,
                        similarity(lower(f_unaccent(ad)), lower(f_unaccent(:q))) AS benzerlik
                    FROM yerlesimler
                    WHERE lower(f_unaccent(ad)) LIKE lower(f_unaccent(:q)) || '%' OR lower(f_unaccent(ad)) % lower(f_unaccent(:q))
                ),
                q_gezi AS (
                    SELECT
                        ad,
                        ad || ' (' || kategori || ')' AS etiket,
                        ST_Y(konum::geometry) AS enlem,
                        ST_X(konum::geometry) AS boylam,
                        0 AS onem,
                        0 AS nufus,
                        2 AS tip_sirasi,
                        CASE WHEN lower(f_unaccent(ad)) LIKE lower(f_unaccent(:q)) || '%' THEN 1 ELSE 0 END AS onek_eslesme,
                        similarity(lower(f_unaccent(ad)), lower(f_unaccent(:q))) AS benzerlik
                    FROM places
                    WHERE tur = 'gezi' AND (lower(f_unaccent(ad)) LIKE lower(f_unaccent(:q)) || '%' OR lower(f_unaccent(ad)) % lower(f_unaccent(:q)))
                )
                SELECT ad, etiket, enlem, boylam
                FROM (
                    SELECT * FROM q_yerlesim
                    UNION ALL
                    SELECT * FROM q_gezi
                ) birlesik
                ORDER BY
                    onek_eslesme DESC,
                    tip_sirasi ASC,
                    onem DESC,
                    nufus DESC NULLS LAST,
                    benzerlik DESC
                LIMIT :limit
                """)
                .param("q", q)
                .param("limit", size)
                .query(KonumSonucu.class)
                .list();

        if (!dbSonuclar.isEmpty()) {
            return dbSonuclar;
        }

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
        } catch (Exception e) {
            log.warn("ORS'a ulaşılamadı veya hata verdi, boş liste dönülüyor: {}", e.getMessage());
            return List.of();
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
