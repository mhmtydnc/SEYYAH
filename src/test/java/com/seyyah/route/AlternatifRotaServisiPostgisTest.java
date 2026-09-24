package com.seyyah.route;

import com.seyyah.PostgisTestDestegi;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// Aday şehir sorgusu ve örtüşme oranı gerçek PostGIS üzerinde test edilir (Docker yoksa atlanır)
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AlternatifRotaServisiPostgisTest extends PostgisTestDestegi {

    // Basit meridyen hattı: kalkış-varış arası ~222 km, hesabı elle doğrulaması kolay
    private static final double K_LON = 30.0, K_LAT = 40.0;
    private static final double V_LON = 30.0, V_LAT = 38.0;
    private static final RotaSonucu ANA = new RotaSonucu(
            "SRID=4326;LINESTRING(30.0 40.0, 30.0 38.0)", List.of(), 222_000, 10_000);

    @Autowired
    private JdbcClient jdbcClient;

    private AlternatifRotaServisi servisi;

    @BeforeEach
    void hazirla() {
        servisi = new AlternatifRotaServisi(mock(OpenRouteService.class), jdbcClient);
    }

    private void yerlesim(long osmId, String ad, String tur, int onem, double lon, double lat) {
        jdbcClient.sql("""
                        INSERT INTO yerlesimler (osm_type, osm_id, ad, tur, onem, konum)
                        VALUES ('n', ?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography)
                        """)
                .params(osmId, ad, tur, onem, lon, lat).update();
    }

    @Test
    void anaHattaCokYakinVeElipsDisindakiVeYanlisTurElenir() {
        yerlesim(1, "TamHatUstunde", "city", 50, 30.0, 39.0);        // hatta tam üstünde -> elenir
        yerlesim(2, "Uygun", "city", 50, 30.3, 39.0);                 // elips içi, hattan yeterince uzak -> kabul
        yerlesim(3, "ElipsDisi", "city", 50, 32.0, 39.0);             // elips dışı (çok dolambaçlı) -> elenir
        yerlesim(4, "KalkisaCokYakin", "town", 50, 30.05, 39.95);      // kalkışa < 20 km -> elenir
        yerlesim(5, "YanlisTur", "village", 50, 30.3, 39.0);          // tur city/town değil -> elenir

        List<AdayKonum> adaylar = servisi.adaySehirleriBul(ANA, K_LON, K_LAT, V_LON, V_LAT);

        assertThat(adaylar).extracting(AdayKonum::ad).containsExactly("Uygun");
    }

    @Test
    void onemVeNufusaGoreSiralanir() {
        yerlesim(1, "OnemYuksek", "city", 80, 30.3, 39.0);
        yerlesim(2, "OnemDusuk", "town", 10, 29.7, 39.0);

        List<AdayKonum> adaylar = servisi.adaySehirleriBul(ANA, K_LON, K_LAT, V_LON, V_LAT);

        assertThat(adaylar).extracting(AdayKonum::ad).containsExactly("OnemYuksek", "OnemDusuk");
    }

    @Test
    void ayniHatTamOrtusurBirDoner() {
        double oran = servisi.ortusmeOrani(
                "SRID=4326;LINESTRING(30.0 40.0, 30.0 38.0)",
                "SRID=4326;LINESTRING(30.0 40.0, 30.0 38.0)");

        assertThat(oran).isCloseTo(1.0, Offset.offset(0.01));
    }

    @Test
    void uzakHatHicOrtusmezSifirDoner() {
        double oran = servisi.ortusmeOrani(
                "SRID=4326;LINESTRING(30.0 40.0, 30.0 38.0)",
                "SRID=4326;LINESTRING(35.0 40.0, 35.0 38.0)");

        assertThat(oran).isEqualTo(0.0);
    }
}
