package com.seyyah.detay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seyyah.PostgisTestDestegi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// Google kotası ve 30 gün kuralı önbellek davranışına bağlı; gerçek PostGIS ile (Docker yoksa atlanır)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({YerDetayServisi.class, YerDetayOnbellekTest.Ayar.class})
class YerDetayOnbellekTest extends PostgisTestDestegi {

    @TestConfiguration
    static class Ayar {
        @Bean
        JdbcClient jdbcClient(DataSource veriKaynagi) {
            return JdbcClient.create(veriKaynagi);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired
    private YerDetayServisi servis;

    @Autowired
    private JdbcClient jdbcClient;

    @MockitoBean
    private WikidataServisi wikidataServisi;

    @MockitoBean
    private GooglePuanServisi googlePuanServisi;

    private long yerId;

    @BeforeEach
    void yerEkle() {
        yerId = jdbcClient.sql("""
                        INSERT INTO places (osm_type, osm_id, ad, kategori, tur, konum)
                        VALUES ('n', 1, 'Kale', 'castle', 'gezi', ST_GeogFromText('SRID=4326;POINT(34.16 39.14)'))
                        RETURNING id
                        """).query(Long.class).single();
    }

    @Test
    void eslesmeYokSonucuOnbellegeYazilirVeKotaTekrarHarcanmaz() {
        given(googlePuanServisi.detayGetir(anyString(), anyDouble(), anyDouble(), any()))
                .willReturn(GoogleServisYaniti.eslesmeYok());

        assertThat(servis.detayGetir(yerId).google()).isNull();
        assertThat(servis.detayGetir(yerId).google()).isNull();

        verify(googlePuanServisi, times(1)).detayGetir(anyString(), anyDouble(), anyDouble(), any());
    }

    @Test
    void hataOlursaOnbellegeYazilmazVeSonrakiIstekteTekrarDenenir() {
        given(googlePuanServisi.detayGetir(anyString(), anyDouble(), anyDouble(), any())).willReturn(null);

        servis.detayGetir(yerId);
        servis.detayGetir(yerId);

        verify(googlePuanServisi, times(2)).detayGetir(anyString(), anyDouble(), anyDouble(), any());
    }

    @Test
    void otuzGunuGecmisGoogleVerisiYenilenemezseGosterilmezVeSilinir() {
        jdbcClient.sql("""
                        INSERT INTO yer_detay_onbellek (yer_id, google_place_id, google, google_zamani)
                        VALUES (?, 'ChIJ-eski', '{"puan": 4.2, "yorumSayisi": 10, "haritaBaglantisi": null}'::jsonb, now() - interval '40 days')
                        """).param(yerId).update();
        given(googlePuanServisi.detayGetir(anyString(), anyDouble(), anyDouble(), any())).willReturn(null);

        assertThat(servis.detayGetir(yerId).google()).isNull();

        Boolean googleBos = jdbcClient.sql("SELECT google IS NULL FROM yer_detay_onbellek WHERE yer_id = ?")
                .param(yerId).query(Boolean.class).single();
        assertThat(googleBos).isTrue();
        // Zaman eski kalır ki bir sonraki istekte yeniden denensin
        Boolean halaEski = jdbcClient.sql("SELECT google_zamani < now() - interval '30 days' FROM yer_detay_onbellek WHERE yer_id = ?")
                .param(yerId).query(Boolean.class).single();
        assertThat(halaEski).isTrue();
    }

    @MockitoBean
    private OzetServisi ozetServisi;

    @Test
    void ozetBirKezUretilirIkinciIstekteCagrilmaz() {
        jdbcClient.sql("UPDATE places SET wikidata_id = 'Q123' WHERE id = ?").param(yerId).update();
        
        WikidataDetay wikidata = new WikidataDetay("aciklama", "https://tr.wikipedia.org/wiki/Test", null);
        given(wikidataServisi.detayGetir("Q123")).willReturn(wikidata);
        
        OzetDetay ozet = new OzetDetay("Özet", "https://tr.wikipedia.org/wiki/Test");
        given(ozetServisi.ozetUret(wikidata)).willReturn(ozet);
        
        assertThat(servis.detayGetir(yerId).ozet()).isNotNull();
        assertThat(servis.detayGetir(yerId).ozet()).isNotNull();
        
        verify(ozetServisi, times(1)).ozetUret(any());
    }

    @Test
    void ozetHataSonucuOnbellegeYazilmaz() {
        jdbcClient.sql("UPDATE places SET wikidata_id = 'Q123' WHERE id = ?").param(yerId).update();
        
        WikidataDetay wikidata = new WikidataDetay("aciklama", "https://tr.wikipedia.org/wiki/Test", null);
        given(wikidataServisi.detayGetir("Q123")).willReturn(wikidata);
        
        given(ozetServisi.ozetUret(wikidata)).willThrow(new RuntimeException("API error"));
        
        assertThat(servis.detayGetir(yerId).ozet()).isNull();
        assertThat(servis.detayGetir(yerId).ozet()).isNull();
        
        verify(ozetServisi, times(2)).ozetUret(any());
    }
}
