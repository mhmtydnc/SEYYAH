package com.seyyah.detay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Service
public class GooglePuanServisi {
    private static final Logger log = LoggerFactory.getLogger(GooglePuanServisi.class);
    private final RestClient restClient;
    private final JdbcClient jdbcClient;
    private final String apiKey;

    public GooglePuanServisi(RestClient.Builder restClientBuilder, 
                             JdbcClient jdbcClient,
                             @Value("${google.places.anahtar:}") String apiKey) {
        this.restClient = restClientBuilder.build();
        this.jdbcClient = jdbcClient;
        this.apiKey = apiKey;
    }

    public GoogleServisYaniti detayGetir(String ad, Double enlem, Double boylam, String bilinenPlaceId) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        if (!kotaArtirVeKontrolEt()) {
            return null;
        }

        try {
            if (bilinenPlaceId == null) {
                // searchText
                String url = "https://places.googleapis.com/v1/places:searchText";
                
                Map<String, Object> body = Map.of(
                        "textQuery", ad,
                        "languageCode", "tr",
                        "maxResultCount", 1,
                        "locationBias", Map.of(
                                "circle", Map.of(
                                        "center", Map.of(
                                                "latitude", enlem,
                                                "longitude", boylam
                                        ),
                                        "radius", 500
                                )
                        )
                );

                Map<String, Object> response = restClient.post()
                        .uri(url)
                        .header("X-Goog-Api-Key", apiKey)
                        .header("X-Goog-FieldMask", "places.id,places.rating,places.userRatingCount,places.googleMapsUri,places.location,places.reviews")
                        .body(body)
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

                if (response == null || !(response.get("places") instanceof List<?> liste) || liste.isEmpty()) {
                    return GoogleServisYaniti.eslesmeYok();
                }
                List<Map<String, Object>> places = (List<Map<String, Object>>) response.get("places");

                Map<String, Object> place = places.get(0);
                // Konumu olmayan ya da 1 km'den uzak sonuç başka bir yerdir; yanlış puan göstermektense hiç gösterme
                Map<String, Object> location = (Map<String, Object>) place.get("location");
                if (location == null || !(location.get("latitude") instanceof Number pLat)
                        || !(location.get("longitude") instanceof Number pLon)
                        || mesafeM(enlem, boylam, pLat.doubleValue(), pLon.doubleValue()) > 1000) {
                    return GoogleServisYaniti.eslesmeYok();
                }
                
                String placeId = (String) place.get("id");
                GoogleDetay detay = parseDetay(place);
                return new GoogleServisYaniti(placeId, detay);

            } else {
                // get by id
                String url = "https://places.googleapis.com/v1/places/" + bilinenPlaceId;
                
                Map<String, Object> place = restClient.get()
                        .uri(url)
                        .header("X-Goog-Api-Key", apiKey)
                        .header("X-Goog-FieldMask", "rating,userRatingCount,googleMapsUri,reviews")
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
                
                GoogleDetay detay = parseDetay(place);
                return new GoogleServisYaniti(bilinenPlaceId, detay);
            }

        } catch (Exception e) {
            log.warn("Google Places hatası ({}): {}", ad, e.getMessage());
            return null;
        }
    }

    private GoogleDetay parseDetay(Map<String, Object> place) {
        if (place == null || !place.containsKey("rating")) {
            return null; // no rating
        }
        Double puan = ((Number) place.get("rating")).doubleValue();
        Integer yorumSayisi = place.containsKey("userRatingCount") ? ((Number) place.get("userRatingCount")).intValue() : 0;
        String harita = (String) place.get("googleMapsUri");
        
        java.util.List<GoogleYorum> yorumlar = new java.util.ArrayList<>();
        if (place.containsKey("reviews") && place.get("reviews") instanceof List<?> rList) {
            for (Object rObj : rList) {
                if (yorumlar.size() >= 3) break;
                if (rObj instanceof Map<?, ?> rMap) {
                    String yazar = null;
                    String yazarBaglantisi = null;
                    if (rMap.get("authorAttribution") instanceof Map<?, ?> authMap) {
                        yazar = (String) authMap.get("displayName");
                        yazarBaglantisi = (String) authMap.get("uri");
                    }
                    Integer rPuan = null;
                    if (rMap.get("rating") instanceof Number num) {
                        rPuan = num.intValue();
                    }
                    String metin = null;
                    if (rMap.get("text") instanceof Map<?, ?> tMap) {
                        metin = (String) tMap.get("text");
                    } else if (rMap.get("originalText") instanceof Map<?, ?> oMap) {
                        metin = (String) oMap.get("text");
                    }
                    if (metin != null && metin.length() > 600) {
                        metin = metin.substring(0, 600);
                    }
                    String zaman = (String) rMap.get("relativePublishTimeDescription");
                    yorumlar.add(new GoogleYorum(yazar, yazarBaglantisi, rPuan, metin, zaman));
                }
            }
        }
        
        return new GoogleDetay(puan, yorumSayisi, harita, yorumlar);
    }

    private boolean kotaArtirVeKontrolEt() {
        String ay = YearMonth.now().toString();
        String sql = """
                INSERT INTO api_kullanim (ay, servis, sayi)
                VALUES (?, 'google_places', 1)
                ON CONFLICT (ay, servis)
                DO UPDATE SET sayi = api_kullanim.sayi + 1
                RETURNING sayi
                """;
        Integer sayi = jdbcClient.sql(sql)
                .param(ay)
                .query(Integer.class)
                .single();
        
        return sayi != null && sayi <= 900;
    }

    private double mesafeM(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000;
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }
}
