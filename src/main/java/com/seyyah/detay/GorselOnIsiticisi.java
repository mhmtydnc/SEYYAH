package com.seyyah.detay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

// Wikidata kaydı olan yerlerin görsel ve açıklamasını önceden önbelleğe alır; böylece rota yanıtı görsel adresini
// taşır ve haritada yere tıklayınca fotoğraf beklemeden görünür. Wikimedia'yı yormamak için dakikada küçük bir parti.
// Yalnızca üretimde açılır (application-prod.yaml); yerelde ve CI'da dış servise istek atılmaz.
@Component
@ConditionalOnProperty(name = "seyyah.gorsel-isitici.etkin", havingValue = "true")
public class GorselOnIsiticisi {

    private static final Logger log = LoggerFactory.getLogger(GorselOnIsiticisi.class);
    private static final int PARTI = 20;

    private final WikidataServisi wikidataServisi;
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public GorselOnIsiticisi(WikidataServisi wikidataServisi, JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.wikidataServisi = wikidataServisi;
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    private record Aday(long id, String wikidataId) {
    }

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1M")
    public void isit() {
        // Önce gezi yerleri ve önemliler: kullanıcının en çok göreceği görseller erken hazır olsun
        List<Aday> adaylar = jdbcClient.sql("""
                        SELECT p.id, p.wikidata_id
                        FROM places p
                        LEFT JOIN yer_detay_onbellek o ON o.yer_id = p.id
                        WHERE p.wikidata_id IS NOT NULL AND (o.yer_id IS NULL OR o.wikidata_zamani IS NULL)
                        ORDER BY (p.tur = 'gezi') DESC, p.onem_skoru DESC, p.id
                        LIMIT ?
                        """)
                .param(PARTI)
                .query((rs, i) -> new Aday(rs.getLong("id"), rs.getString("wikidata_id")))
                .list();
        if (adaylar.isEmpty()) {
            return;
        }

        int gorselli = 0;
        for (Aday aday : adaylar) {
            WikidataDetay detay = wikidataServisi.detayGetir(aday.wikidataId());
            // Çekilemeyen yer "29 gün önce" diye işaretlenir: ısıtıcı bir daha seçmez, detay açılınca bir gün
            // sonra yeniden denenir. Aksi hâlde hep aynı başarısız yerler partiyi doldurup ilerlemeyi durdururdu.
            OffsetDateTime zaman = detay != null ? OffsetDateTime.now() : OffsetDateTime.now().minusDays(29);
            yaz(aday.id(), detay, zaman);
            if (detay != null && detay.gorsel() != null) {
                gorselli++;
            }
        }
        log.info("Görsel ön ısıtma: {} yer işlendi, {} tanesinde görsel var", adaylar.size(), gorselli);
    }

    private void yaz(long yerId, WikidataDetay detay, OffsetDateTime zaman) {
        String json;
        try {
            json = detay == null ? null : objectMapper.writeValueAsString(detay);
        } catch (JsonProcessingException e) {
            json = null;
        }
        // Yalnızca Wikidata sütunlarına dokunulur; Google ve özet önbelleği korunur
        jdbcClient.sql("""
                        INSERT INTO yer_detay_onbellek (yer_id, wikidata, wikidata_zamani)
                        VALUES (?, CAST(? AS jsonb), ?)
                        ON CONFLICT (yer_id) DO UPDATE SET
                            wikidata = EXCLUDED.wikidata,
                            wikidata_zamani = EXCLUDED.wikidata_zamani
                        """)
                .params(yerId, json, zaman)
                .update();
    }
}
