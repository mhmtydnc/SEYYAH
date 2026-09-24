package com.seyyah.detay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OzetServisi {
    private static final Logger log = LoggerFactory.getLogger(OzetServisi.class);
    private final RestClient restClient;
    private final JdbcClient jdbcClient;
    private final String apiKey;
    private final String model;

    public OzetServisi(RestClient.Builder restClientBuilder,
                       JdbcClient jdbcClient,
                       @Value("${gemini.anahtar:}") String apiKey,
                       @Value("${gemini.model:gemini-flash-lite-latest}") String model) {
        this.restClient = restClientBuilder.build();
        this.jdbcClient = jdbcClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    // Anahtar yoksa servis kapalıdır; çağıran bunu "özet yok" sanıp süresiz önbelleğe yazmamalı
    public boolean etkin() {
        return apiKey != null && !apiKey.isBlank();
    }

    public OzetDetay ozetUret(WikidataDetay wikidata) {
        if (!etkin()) {
            return null;
        }
        if (wikidata == null || wikidata.vikipedi() == null || wikidata.vikipedi().isBlank()) {
            return null;
        }

        String vikiUrl = wikidata.vikipedi();
        Pattern pattern = Pattern.compile("https://([a-z-]+)\\.wikipedia\\.org/wiki/(.+)");
        Matcher matcher = pattern.matcher(vikiUrl);
        if (!matcher.matches()) {
            return null;
        }
        String lang = matcher.group(1);
        String titleEncoded = matcher.group(2);
        String title = URLDecoder.decode(titleEncoded, StandardCharsets.UTF_8);

        String extract;
        try {
            URI wikiUri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host(lang + ".wikipedia.org")
                    .pathSegment("api", "rest_v1", "page", "summary", title)
                    .encode()
                    .build()
                    .toUri();

            Map<String, Object> wikiResp = restClient.get()
                    .uri(wikiUri)
                    .header("User-Agent", "Seyyah/1.0 (https://github.com/mhmtydnc/SEYYAH)")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (wikiResp == null || !wikiResp.containsKey("extract")) {
                return null;
            }
            extract = (String) wikiResp.get("extract");
        } catch (Exception e) {
            log.warn("Wikipedia özet alınamadı: {}", e.getMessage());
            throw new RuntimeException("Wiki error", e);
        }

        if (extract == null || extract.length() < 80) {
            return null;
        }

        String metin = extract;
        if (metin.length() > 2000) {
            metin = metin.substring(0, 2000);
        }

        if (!kotaArtirVeKontrolEt()) {
            throw new RuntimeException("Gemini quota error");
        }

        try {
            String istem = "Aşağıdaki Vikipedi metnine dayanarak bir gezgin için 2-3 cümlelik, Türkçe, abartısız bir tanıtım yaz: yerin ne olduğu ve neden görülmeye değer olduğu. Metinde olmayan bilgi ekleme.\n\nMETİN:\n" + metin;

            URI geminiUri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("generativelanguage.googleapis.com")
                    .path("/v1beta/models/" + model + ":generateContent")
                    .encode()
                    .build()
                    .toUri();

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", istem)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "maxOutputTokens", 200,
                            "temperature", 0.3
                    )
            );

            Map<String, Object> geminiResp = restClient.post()
                    .uri(geminiUri)
                    .header("x-goog-api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (geminiResp == null || !geminiResp.containsKey("candidates")) {
                throw new RuntimeException("Gemini invalid response");
            }
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) geminiResp.get("candidates");
            if (candidates.isEmpty()) throw new RuntimeException("Gemini no candidates");

            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts.isEmpty()) throw new RuntimeException("Gemini no parts");

            String sonuc = (String) parts.get(0).get("text");
            if (sonuc == null) throw new RuntimeException("Gemini null text");

            sonuc = sonuc.replaceAll("[*#_]", "");
            if (sonuc.length() > 600) {
                sonuc = sonuc.substring(0, 600);
            }

            return new OzetDetay(sonuc.trim(), vikiUrl);
        } catch (Exception e) {
            log.warn("Gemini hatası: {}", e.getMessage());
            throw new RuntimeException("Gemini error", e);
        }
    }

    private boolean kotaArtirVeKontrolEt() {
        String gun = LocalDate.now().toString();
        String sql = """
                INSERT INTO api_kullanim (ay, servis, sayi)
                VALUES (?, 'gemini', 1)
                ON CONFLICT (ay, servis)
                DO UPDATE SET sayi = api_kullanim.sayi + 1
                RETURNING sayi
                """;
        Integer sayi = jdbcClient.sql(sql)
                .param(gun)
                .query(Integer.class)
                .single();
        
        return sayi != null && sayi <= 300;
    }
}
