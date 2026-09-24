package com.seyyah.detay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class WikidataServisi {
    private static final Logger log = LoggerFactory.getLogger(WikidataServisi.class);
    private final RestClient restClient;
    private static final String USER_AGENT = "Seyyah/1.0 (https://github.com/mhmtydnc/SEYYAH)";

    public WikidataServisi(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public WikidataDetay detayGetir(String wikidataId) {
        if (wikidataId == null || wikidataId.isBlank()) {
            return null;
        }
        try {
            String url = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" + wikidataId +
                    "&props=descriptions|claims|sitelinks&languages=tr|en&format=json";
            
            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey("entities")) {
                return null;
            }
            
            Map<String, Object> entities = (Map<String, Object>) response.get("entities");
            Map<String, Object> entity = (Map<String, Object>) entities.get(wikidataId);
            if (entity == null) {
                return null;
            }

            String aciklama = null;
            Map<String, Object> descriptions = (Map<String, Object>) entity.get("descriptions");
            if (descriptions != null) {
                if (descriptions.containsKey("tr")) {
                    aciklama = (String) ((Map<String, Object>) descriptions.get("tr")).get("value");
                } else if (descriptions.containsKey("en")) {
                    aciklama = (String) ((Map<String, Object>) descriptions.get("en")).get("value");
                }
            }

            String vikipedi = null;
            Map<String, Object> sitelinks = (Map<String, Object>) entity.get("sitelinks");
            if (sitelinks != null) {
                if (sitelinks.containsKey("trwiki")) {
                    String title = (String) ((Map<String, Object>) sitelinks.get("trwiki")).get("title");
                    vikipedi = "https://tr.wikipedia.org/wiki/" + UriUtils.encodePathSegment(title.replace(" ", "_"), StandardCharsets.UTF_8);
                } else if (sitelinks.containsKey("enwiki")) {
                    String title = (String) ((Map<String, Object>) sitelinks.get("enwiki")).get("title");
                    vikipedi = "https://en.wikipedia.org/wiki/" + UriUtils.encodePathSegment(title.replace(" ", "_"), StandardCharsets.UTF_8);
                }
            }

            GorselDetay gorsel = null;
            Map<String, Object> claims = (Map<String, Object>) entity.get("claims");
            if (claims != null && claims.containsKey("P18")) {
                List<Map<String, Object>> p18List = (List<Map<String, Object>>) claims.get("P18");
                if (!p18List.isEmpty()) {
                    Map<String, Object> mainsnak = (Map<String, Object>) p18List.get(0).get("mainsnak");
                    if (mainsnak != null && mainsnak.containsKey("datavalue")) {
                        Map<String, Object> datavalue = (Map<String, Object>) mainsnak.get("datavalue");
                        String fileName = (String) datavalue.get("value");
                        gorsel = gorselGetir(fileName);
                    }
                }
            }

            return new WikidataDetay(aciklama, vikipedi, gorsel);

        } catch (Exception e) {
            log.warn("Wikidata hatası ({}): {}", wikidataId, e.getMessage());
            return null;
        }
    }

    private GorselDetay gorselGetir(String fileName) {
        try {
            // URI hazır nesne olarak verilir: uri(String) metni şablon sayıp yeniden kodluyordu (% -> %25),
            // boşluklu ya da Türkçe karakterli dosya adları Commons'ta bulunamıyor ve görsel sessizce kayboluyordu
            URI url = UriComponentsBuilder.fromUriString("https://commons.wikimedia.org/w/api.php")
                    .queryParam("action", "query")
                    .queryParam("titles", "File:" + fileName)
                    .queryParam("prop", "imageinfo")
                    .queryParam("iiprop", "url|extmetadata")
                    .queryParam("iiurlwidth", 640)
                    .queryParam("format", "json")
                    .encode()
                    .build()
                    .toUri();

            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey("query")) return null;
            Map<String, Object> query = (Map<String, Object>) response.get("query");
            Map<String, Object> pages = (Map<String, Object>) query.get("pages");
            if (pages == null || pages.isEmpty()) return null;

            Map<String, Object> page = (Map<String, Object>) pages.values().iterator().next();
            if (!page.containsKey("imageinfo")) return null;

            List<Map<String, Object>> imageinfoList = (List<Map<String, Object>>) page.get("imageinfo");
            if (imageinfoList.isEmpty()) return null;

            Map<String, Object> imageinfo = imageinfoList.get(0);
            
            String thumburl = (String) imageinfo.get("thumburl");
            String descriptionurl = (String) imageinfo.get("descriptionurl");
            
            String yazar = null;
            String lisans = null;
            
            Map<String, Object> extmetadata = (Map<String, Object>) imageinfo.get("extmetadata");
            if (extmetadata != null) {
                if (extmetadata.containsKey("Artist")) {
                    String artistHtml = (String) ((Map<String, Object>) extmetadata.get("Artist")).get("value");
                    if (artistHtml != null) {
                        yazar = artistHtml.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
                        if (yazar.length() > 200) {
                            yazar = yazar.substring(0, 200);
                        }
                    }
                }
                if (extmetadata.containsKey("LicenseShortName")) {
                    lisans = (String) ((Map<String, Object>) extmetadata.get("LicenseShortName")).get("value");
                }
            }

            return new GorselDetay(thumburl, descriptionurl, yazar, lisans);
        } catch (Exception e) {
            log.warn("Commons görsel hatası ({}): {}", fileName, e.getMessage());
            return null;
        }
    }
}
