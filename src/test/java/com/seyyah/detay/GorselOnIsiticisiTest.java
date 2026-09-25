package com.seyyah.detay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seyyah.PostgisTestDestegi;
import com.seyyah.place.KoridorYeri;
import com.seyyah.place.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// Ön ısıtılan görsel rota yanıtına gelmeli; çekilemeyen yer partiyi tıkamamalı (Docker yoksa atlanır)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GorselOnIsiticisi.class, GorselOnIsiticisiTest.Ayar.class})
@TestPropertySource(properties = "seyyah.gorsel-isitici.etkin=true")
class GorselOnIsiticisiTest extends PostgisTestDestegi {

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

    private static final String ROTA = "SRID=4326;LINESTRING(29.0 41.0, 29.0 41.1)";

    @Autowired
    private GorselOnIsiticisi isitici;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @MockitoBean
    private WikidataServisi wikidataServisi;

    @BeforeEach
    void yerEkle() {
        jdbcClient.sql("""
                INSERT INTO places (osm_type, osm_id, ad, kategori, tur, wikidata_id, konum)
                VALUES ('n', 77, 'Kale', 'castle', 'gezi', 'Q77', ST_GeogFromText('SRID=4326;POINT(29.0 41.05)'))
                """).update();
    }

    @Test
    void isitilanGorselRotaYanitindaGelir() {
        given(wikidataServisi.detayGetir("Q77")).willReturn(new WikidataDetay("Bir kale", null,
                new GorselDetay("https://upload.wikimedia.org/kale.jpg", "https://commons.wikimedia.org/kale", "A", "CC BY")));

        assertThat(placeRepository.koridorda(ROTA, "gezi", 500, 10))
                .extracting(KoridorYeri::getGorselUrl).containsExactly((String) null);

        isitici.isit();

        List<KoridorYeri> sonuc = placeRepository.koridorda(ROTA, "gezi", 500, 10);
        assertThat(sonuc).extracting(KoridorYeri::getGorselUrl).containsExactly("https://upload.wikimedia.org/kale.jpg");
    }

    @Test
    void cekilemeyenYerTekrarSecilmez() {
        given(wikidataServisi.detayGetir("Q77")).willReturn(null);

        isitici.isit();
        isitici.isit();

        verify(wikidataServisi, times(1)).detayGetir("Q77");
    }
}
