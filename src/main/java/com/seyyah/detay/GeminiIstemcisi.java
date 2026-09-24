package com.seyyah.detay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiIstemcisi {
    private static final Logger log = LoggerFactory.getLogger(GeminiIstemcisi.class);
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiIstemcisi(RestClient.Builder restClientBuilder,
                           @Value("${gemini.anahtar:}") String apiKey,
                           @Value("${gemini.model:gemini-flash-lite-latest}") String model) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public boolean etkin() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String icerikUret(String systemInstruction, String userMessage) {
        if (!etkin()) {
            throw new RuntimeException("Gemini API anahtarı eksik");
        }

        try {
            URI geminiUri = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("generativelanguage.googleapis.com")
                    .path("/v1beta/models/" + model + ":generateContent")
                    .encode()
                    .build()
                    .toUri();

            Map<String, Object> body = new HashMap<>();
            
            body.put("contents", List.of(
                    Map.of("parts", List.of(
                            Map.of("text", userMessage)
                    ))
            ));
            
            if (systemInstruction != null && !systemInstruction.isBlank()) {
                body.put("systemInstruction", Map.of(
                        "parts", List.of(
                                Map.of("text", systemInstruction)
                        )
                ));
            }
            
            body.put("generationConfig", Map.of(
                    "maxOutputTokens", 200,
                    "temperature", 0.3
            ));

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

            return sonuc;
        } catch (Exception e) {
            log.warn("Gemini hatası: {}", e.getMessage());
            throw new RuntimeException("Gemini error", e);
        }
    }
}
