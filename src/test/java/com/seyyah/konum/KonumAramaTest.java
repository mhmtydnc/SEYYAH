package com.seyyah.konum;

import com.seyyah.PostgisTestDestegi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Arama SQL'i gerçek PostGIS + unaccent + pg_trgm üzerinde (Docker yoksa atlanır)
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KonumAramaTest extends PostgisTestDestegi {

    @Autowired
    private JdbcClient jdbcClient;

    private KonumServisi servis;

    @BeforeEach
    void hazirla() {
        // Kendi verimizde sonuç varken ORS'a hiç gidilmez; adres bilerek erişilemez
        servis = new KonumServisi(RestClient.builder(), jdbcClient, "http://127.0.0.1:1", "anahtar");
    }

    private void yerlesim(long osmId, String ad, String tur, String ilce, String il, int onem) {
        jdbcClient.sql("""
                        INSERT INTO yerlesimler (osm_type, osm_id, ad, tur, ilce, il, onem, konum)
                        VALUES ('n', ?, ?, ?, ?, ?, ?, ST_GeogFromText('SRID=4326;POINT(30 39)'))
                        """)
                .params(osmId, ad, tur, ilce, il, onem).update();
    }

    @Test
    void turkceKarakterlerdenBagimsizBulur() {
        yerlesim(1, "İstanbul", "city", "Fatih", "İstanbul", 100);

        assertThat(servis.ara("istan", 5)).extracting(KonumSonucu::ad).containsExactly("İstanbul");
        assertThat(servis.ara("ISTANBUL", 5)).extracting(KonumSonucu::ad).containsExactly("İstanbul");
    }

    @Test
    void etiketIlceVeIliGereksizTekrarOlmadanGosterir() {
        yerlesim(1, "Ankara", "city", "Çankaya", "Ankara", 100);
        yerlesim(2, "Göreme", "village", "Nevşehir Merkez", "Nevşehir", 10);
        yerlesim(3, "Yeniköy", "village", "Şile", "İstanbul", 10);

        assertThat(servis.ara("ankara", 5).get(0).etiket()).isEqualTo("Ankara");
        assertThat(servis.ara("goreme", 5).get(0).etiket()).isEqualTo("Göreme, Nevşehir");
        assertThat(servis.ara("yenikoy", 5).get(0).etiket()).isEqualTo("Yeniköy, Şile, İstanbul");
    }

    @Test
    void onekEslesmesiVeOnemSiralamayiBelirler() {
        yerlesim(1, "Kars Köyü", "village", "Merkez", "Kars", 10);
        yerlesim(2, "Kars", "city", "Merkez", "Kars", 100);
        jdbcClient.sql("""
                        INSERT INTO places (osm_type, osm_id, ad, kategori, tur, konum)
                        VALUES ('n', 9, 'Kars Kalesi', 'castle', 'gezi', ST_GeogFromText('SRID=4326;POINT(43 40)'))
                        """).update();

        List<String> adlar = servis.ara("kars", 5).stream().map(KonumSonucu::ad).toList();

        assertThat(adlar).containsExactly("Kars", "Kars Köyü", "Kars Kalesi");
    }
}
