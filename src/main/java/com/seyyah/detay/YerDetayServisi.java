package com.seyyah.detay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seyyah.hata.ApiIstisnasi;
import com.seyyah.place.Place;
import com.seyyah.place.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

@Service
public class YerDetayServisi {
    private static final Logger log = LoggerFactory.getLogger(YerDetayServisi.class);
    
    private final PlaceRepository placeRepository;
    private final WikidataServisi wikidataServisi;
    private final GooglePuanServisi googlePuanServisi;
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public YerDetayServisi(PlaceRepository placeRepository,
                           WikidataServisi wikidataServisi,
                           GooglePuanServisi googlePuanServisi,
                           JdbcClient jdbcClient,
                           ObjectMapper objectMapper) {
        this.placeRepository = placeRepository;
        this.wikidataServisi = wikidataServisi;
        this.googlePuanServisi = googlePuanServisi;
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public YerDetay detayGetir(Long id) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new ApiIstisnasi(HttpStatus.NOT_FOUND, "Yer bulunamadı"));

        OnbellekKaydi onbellek = onbellekGetir(id);
        
        WikidataDetay wikidata = null;
        GoogleDetay google = null;
        String googlePlaceId = null;
        
        boolean wikidataGuncelle = false;
        boolean googleGuncelle = false;

        OffsetDateTime simdi = OffsetDateTime.now();
        OffsetDateTime otuzGunOnce = simdi.minusDays(30);

        if (onbellek != null) {
            wikidata = onbellek.wikidata;
            google = onbellek.google;
            googlePlaceId = onbellek.googlePlaceId;
            
            if (onbellek.wikidataZamani == null || onbellek.wikidataZamani.isBefore(otuzGunOnce)) {
                wikidataGuncelle = true;
            }
            if (onbellek.googleZamani == null || onbellek.googleZamani.isBefore(otuzGunOnce)) {
                googleGuncelle = true;
            }
        } else {
            wikidataGuncelle = true;
            googleGuncelle = true;
        }

        if (wikidataGuncelle && place.getWikidataId() != null) {
            WikidataDetay yeniWikidata = wikidataServisi.detayGetir(place.getWikidataId());
            if (yeniWikidata != null) {
                wikidata = yeniWikidata;
            } else {
                wikidataGuncelle = false;
            }
        } else if (place.getWikidataId() == null) {
            wikidataGuncelle = false; // No wikidata id, no need to update or write back true
        }

        // googleGuncelle: yenileme gerekiyordu. googleYenilendi: gerçekten yenilendi (eşleşme yok sonucu dahil).
        // googleSilindi: yenilenemedi ve eldeki veri 30 günü geçmişti; Google kuralı gereği gösterilmez ve silinir.
        // Silinen kaydın zamanı eski kalır ki bir sonraki istekte yeniden denensin.
        boolean googleYenilendi = false;
        boolean googleSilindi = false;
        if (googleGuncelle) {
            GoogleServisYaniti yanit = googlePuanServisi.detayGetir(
                    place.getAd(), place.getKonum().getY(), place.getKonum().getX(), googlePlaceId);
            if (yanit != null) {
                google = yanit.detay();
                if (yanit.placeId() != null) {
                    googlePlaceId = yanit.placeId();
                }
                googleYenilendi = true;
            } else if (google != null) {
                google = null;
                googleSilindi = true;
            }
        }

        if (wikidataGuncelle || googleYenilendi || googleSilindi || onbellek == null) {
            onbellekKaydet(id, wikidata, wikidataGuncelle ? simdi : (onbellek != null ? onbellek.wikidataZamani : null),
                           googlePlaceId, google, googleYenilendi ? simdi : (onbellek != null ? onbellek.googleZamani : null));
        }

        return new YerDetay(
                place.getId(),
                place.getAd(),
                place.getKategori(),
                place.getTur(),
                place.getKonum().getY(),
                place.getKonum().getX(),
                place.getUcret(),
                place.getCalismaSaatleri(),
                place.getWebsite(),
                wikidata,
                google
        );
    }

    private OnbellekKaydi onbellekGetir(Long id) {
        String sql = "SELECT wikidata, wikidata_zamani, google_place_id, google, google_zamani FROM yer_detay_onbellek WHERE yer_id = ?";
        return jdbcClient.sql(sql)
                .param(id)
                .query(this::mapOnbellek)
                .optional()
                .orElse(null);
    }

    private OnbellekKaydi mapOnbellek(ResultSet rs, int rowNum) throws SQLException {
        OnbellekKaydi k = new OnbellekKaydi();
        try {
            String wdJson = rs.getString("wikidata");
            if (wdJson != null) {
                k.wikidata = objectMapper.readValue(wdJson, WikidataDetay.class);
            }
            k.wikidataZamani = rs.getObject("wikidata_zamani", OffsetDateTime.class);
            k.googlePlaceId = rs.getString("google_place_id");
            
            String ggJson = rs.getString("google");
            if (ggJson != null) {
                k.google = objectMapper.readValue(ggJson, GoogleDetay.class);
            }
            k.googleZamani = rs.getObject("google_zamani", OffsetDateTime.class);
        } catch (JsonProcessingException e) {
            log.warn("Önbellek JSON parse hatası", e);
        }
        return k;
    }

    private void onbellekKaydet(Long id, WikidataDetay wikidata, OffsetDateTime wdZaman,
                                String googlePlaceId, GoogleDetay google, OffsetDateTime ggZaman) {
        String wdJson = null;
        String ggJson = null;
        try {
            if (wikidata != null) wdJson = objectMapper.writeValueAsString(wikidata);
            if (google != null) ggJson = objectMapper.writeValueAsString(google);
        } catch (JsonProcessingException e) {
            log.warn("Önbellek JSON yazma hatası", e);
        }

        String sql = """
                INSERT INTO yer_detay_onbellek (yer_id, wikidata, wikidata_zamani, google_place_id, google, google_zamani)
                VALUES (?, ?::jsonb, ?, ?, ?::jsonb, ?)
                ON CONFLICT (yer_id) DO UPDATE SET
                    wikidata = EXCLUDED.wikidata,
                    wikidata_zamani = EXCLUDED.wikidata_zamani,
                    google_place_id = EXCLUDED.google_place_id,
                    google = EXCLUDED.google,
                    google_zamani = EXCLUDED.google_zamani
                """;
        jdbcClient.sql(sql)
                .param(id)
                .param(wdJson)
                .param(wdZaman)
                .param(googlePlaceId)
                .param(ggJson)
                .param(ggZaman)
                .update();
    }

    private static class OnbellekKaydi {
        WikidataDetay wikidata;
        OffsetDateTime wikidataZamani;
        String googlePlaceId;
        GoogleDetay google;
        OffsetDateTime googleZamani;
    }
}
