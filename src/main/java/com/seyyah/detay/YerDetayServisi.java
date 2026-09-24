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
    private final OzetServisi ozetServisi;
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public YerDetayServisi(PlaceRepository placeRepository,
                           WikidataServisi wikidataServisi,
                           GooglePuanServisi googlePuanServisi,
                           OzetServisi ozetServisi,
                           JdbcClient jdbcClient,
                           ObjectMapper objectMapper) {
        this.placeRepository = placeRepository;
        this.wikidataServisi = wikidataServisi;
        this.googlePuanServisi = googlePuanServisi;
        this.ozetServisi = ozetServisi;
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

        OzetDetay ozet = null;
        boolean ozetGuncelle = false;
        boolean ozetBasarili = false; // error means we don't save to db
        
        if (onbellek != null) {
            wikidata = onbellek.wikidata;
            google = onbellek.google;
            googlePlaceId = onbellek.googlePlaceId;
            ozet = onbellek.ozet;
            
            if (onbellek.wikidataZamani == null || onbellek.wikidataZamani.isBefore(otuzGunOnce)) {
                wikidataGuncelle = true;
            }
            if (onbellek.googleZamani == null || onbellek.googleZamani.isBefore(otuzGunOnce)) {
                googleGuncelle = true;
            }
            if (onbellek.ozetZamani == null) {
                ozetGuncelle = true;
            }
        } else {
            wikidataGuncelle = true;
            googleGuncelle = true;
            ozetGuncelle = true;
        }

        if (wikidataGuncelle && place.getWikidataId() != null) {
            WikidataDetay yeniWikidata = wikidataServisi.detayGetir(place.getWikidataId());
            if (yeniWikidata != null) {
                wikidata = yeniWikidata;
            } else {
                wikidataGuncelle = false;
            }
        } else if (place.getWikidataId() == null) {
            wikidataGuncelle = false;
        }

        if (ozetGuncelle && wikidata != null) {
            try {
                ozet = ozetServisi.ozetUret(wikidata);
                ozetBasarili = true; // No error thrown, we can save the state (either null or result)
            } catch (RuntimeException e) {
                ozetGuncelle = false; // Error happened, don't update db time
                ozetBasarili = false;
            }
        } else if (wikidata == null) {
            ozetGuncelle = false; // No wikidata, nothing to summarize
        }

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

        if (wikidataGuncelle || googleYenilendi || googleSilindi || ozetBasarili || onbellek == null) {
            onbellekKaydet(id, wikidata, wikidataGuncelle ? simdi : (onbellek != null ? onbellek.wikidataZamani : null),
                           googlePlaceId, google, googleYenilendi ? simdi : (onbellek != null ? onbellek.googleZamani : null),
                           ozet, ozetBasarili ? simdi : (onbellek != null ? onbellek.ozetZamani : null));
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
                google,
                ozet
        );
    }

    private OnbellekKaydi onbellekGetir(Long id) {
        String sql = "SELECT wikidata, wikidata_zamani, google_place_id, google, google_zamani, ozet, ozet_zamani FROM yer_detay_onbellek WHERE yer_id = ?";
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
            
            String ozJson = rs.getString("ozet");
            if (ozJson != null) {
                k.ozet = objectMapper.readValue(ozJson, OzetDetay.class);
            }
            k.ozetZamani = rs.getObject("ozet_zamani", OffsetDateTime.class);
        } catch (JsonProcessingException e) {
            log.warn("Önbellek JSON parse hatası", e);
        }
        return k;
    }

    private void onbellekKaydet(Long id, WikidataDetay wikidata, OffsetDateTime wdZaman,
                                String googlePlaceId, GoogleDetay google, OffsetDateTime ggZaman,
                                OzetDetay ozet, OffsetDateTime ozetZamani) {
        String wdJson = null;
        String ggJson = null;
        String ozJson = null;
        try {
            if (wikidata != null) wdJson = objectMapper.writeValueAsString(wikidata);
            if (google != null) ggJson = objectMapper.writeValueAsString(google);
            if (ozet != null) ozJson = objectMapper.writeValueAsString(ozet);
        } catch (JsonProcessingException e) {
            log.warn("Önbellek JSON yazma hatası", e);
        }

        String sql = """
                INSERT INTO yer_detay_onbellek (yer_id, wikidata, wikidata_zamani, google_place_id, google, google_zamani, ozet, ozet_zamani)
                VALUES (?, ?::jsonb, ?, ?, ?::jsonb, ?, ?::jsonb, ?)
                ON CONFLICT (yer_id) DO UPDATE SET
                    wikidata = EXCLUDED.wikidata,
                    wikidata_zamani = EXCLUDED.wikidata_zamani,
                    google_place_id = EXCLUDED.google_place_id,
                    google = EXCLUDED.google,
                    google_zamani = EXCLUDED.google_zamani,
                    ozet = EXCLUDED.ozet,
                    ozet_zamani = EXCLUDED.ozet_zamani
                """;
        jdbcClient.sql(sql)
                .param(id)
                .param(wdJson)
                .param(wdZaman)
                .param(googlePlaceId)
                .param(ggJson)
                .param(ggZaman)
                .param(ozJson)
                .param(ozetZamani)
                .update();
    }

    private static class OnbellekKaydi {
        WikidataDetay wikidata;
        OffsetDateTime wikidataZamani;
        String googlePlaceId;
        GoogleDetay google;
        OffsetDateTime googleZamani;
        OzetDetay ozet;
        OffsetDateTime ozetZamani;
    }
}
