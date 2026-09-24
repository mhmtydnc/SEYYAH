package com.seyyah.place;

import com.seyyah.PostgisTestDestegi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

// Rota: yaklaşık kuzey-güney, ~11,1 km uzunluğunda, lon=29.0 üzerinde (nokta-hatta mesafe ~0)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlaceRepositoryTest extends PostgisTestDestegi {

    private static final String ROTA_WKT = "SRID=4326;LINESTRING(29.000000 41.000000, 29.000000 41.100000)";

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void yerEkle(long id, String ad, String tur, double lon, double lat, int skor) {
        String wkt = String.format(Locale.ROOT, "SRID=4326;POINT(%f %f)", lon, lat);
        jdbcTemplate.update("""
                INSERT INTO places (osm_type, osm_id, ad, kategori, konum, tur, onem_skoru)
                VALUES ('n', ?, ?, 'test-kategori', ST_GeogFromText(?), ?, ?)
                """, id, ad, wkt, tur, skor);
    }

    @Test
    void yaricapDisindakiYerGelmez() {
        yerEkle(1, "Yol Uzeri", "gezi", 29.000000, 41.050000, 0); // ~0 m, hatta çok yakın
        yerEkle(2, "Uzak Yer", "gezi", 29.050000, 41.050000, 0); // ~4,2 km uzak, yarıçap dışı

        List<KoridorYeri> sonuc = placeRepository.koridorda(ROTA_WKT, "gezi", 500, 10);

        assertThat(sonuc).extracting(KoridorYeri::getAd).containsExactly("Yol Uzeri");
    }

    @Test
    void turFiltresiCalisir() {
        yerEkle(1, "Gezi Yeri", "gezi", 29.000000, 41.050000, 0);
        yerEkle(2, "Mola Yeri", "mola", 29.000000, 41.051000, 0);

        List<KoridorYeri> geziSonucu = placeRepository.koridorda(ROTA_WKT, "gezi", 500, 10);
        List<KoridorYeri> molaSonucu = placeRepository.koridorda(ROTA_WKT, "mola", 500, 10);

        assertThat(geziSonucu).extracting(KoridorYeri::getAd).containsExactly("Gezi Yeri");
        assertThat(molaSonucu).extracting(KoridorYeri::getAd).containsExactly("Mola Yeri");
    }

    @Test
    void sonucYolOraninaGoreArtanSiradadir() {
        yerEkle(1, "Uc", "gezi", 29.000000, 41.090000, 0);
        yerEkle(2, "Bir", "gezi", 29.000000, 41.010000, 0);
        yerEkle(3, "Iki", "gezi", 29.000000, 41.050000, 0);

        List<KoridorYeri> sonuc = placeRepository.koridorda(ROTA_WKT, "gezi", 500, 10);

        assertThat(sonuc).extracting(KoridorYeri::getAd).containsExactly("Bir", "Iki", "Uc");
        assertThat(sonuc).extracting(KoridorYeri::getYolOrani).isSorted();
    }

    @Test
    void limitUygulanir() {
        for (long i = 1; i <= 6; i++) {
            yerEkle(i, "Yer" + i, "gezi", 29.000000, 41.000000 + i * 0.01, 0);
        }

        List<KoridorYeri> sonuc = placeRepository.koridorda(ROTA_WKT, "gezi", 500, 3);

        assertThat(sonuc).hasSize(3);
    }

    @Test
    void rotaBoyuncaBolumlereDengeliDagilir() {
        // Başlangıca yakın (oran ~0 - 0.1) 10 yüksek skorlu yer
        for (long i = 1; i <= 10; i++) {
            yerEkle(i, "Baslangic" + i, "gezi", 29.000000, 41.0005 + i * 0.0005, 101 - (int) i);
        }
        // Sona yakın (oran ~0.995) tek düşük skorlu yer
        yerEkle(11, "Bitis", "gezi", 29.000000, 41.0995, 1);

        // limit=4 -> bölümSayısı = (4+1)/2 = 2: [0, 0.5) ve [0.5, 1]; her bölüme en az bir yer düşer
        List<KoridorYeri> sonuc = placeRepository.koridorda(ROTA_WKT, "gezi", 500, 4);

        assertThat(sonuc).hasSize(4);
        assertThat(sonuc).extracting(KoridorYeri::getAd).contains("Bitis");
    }
}
