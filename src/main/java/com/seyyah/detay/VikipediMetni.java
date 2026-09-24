package com.seyyah.detay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VikipediMetni {
    private static final Logger log = LoggerFactory.getLogger(VikipediMetni.class);
    private final RestClient restClient;

    public VikipediMetni(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public String metinGetir(String vikiUrl) {
        if (vikiUrl == null || vikiUrl.isBlank()) {
            return null;
        }

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

        return extract;
    }
}
