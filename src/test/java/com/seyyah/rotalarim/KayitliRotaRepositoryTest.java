package com.seyyah.rotalarim;

import com.seyyah.PostgisTestDestegi;
import com.seyyah.kullanici.Kullanici;
import com.seyyah.kullanici.KullaniciRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KayitliRotaRepositoryTest extends PostgisTestDestegi {

    @Autowired
    private KullaniciRepository kullaniciRepository;

    @Autowired
    private KayitliRotaRepository kayitliRotaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long kullaniciOlustur(String eposta) {
        Kullanici k = new Kullanici();
        k.setAd("Mehmet");
        k.setEposta(eposta);
        k.setSifreOzeti("hash");
        return kullaniciRepository.saveAndFlush(k).getId();
    }

    // @CreationTimestamp Java tarafında set edildiğinden olusturulma'yı testte kontrol edebilmek için
    // satır doğrudan JdbcTemplate ile eklenir
    private void rotaEkle(Long kullaniciId, String baslik, OffsetDateTime olusturulma) {
        jdbcTemplate.update("""
                INSERT INTO kayitli_rotalar
                    (kullanici_id, baslik, kalkis_ad, kalkis_enlem, kalkis_boylam,
                     varis_ad, varis_enlem, varis_boylam, olusturulma)
                VALUES (?, ?, 'Istanbul', 41.0, 29.0, 'Goreme', 38.6, 34.8, ?)
                """, kullaniciId, baslik, olusturulma);
    }

    @Test
    void kullaniciSilinirseRotalariCascadeIleSilinir() {
        Long id = kullaniciOlustur("mehmet@ornek.com");
        rotaEkle(id, "Rota 1", OffsetDateTime.now());
        rotaEkle(id, "Rota 2", OffsetDateTime.now());

        kullaniciRepository.deleteById(id);
        kullaniciRepository.flush();

        Integer kalan = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM kayitli_rotalar WHERE kullanici_id = ?", Integer.class, id);
        assertThat(kalan).isZero();
    }

    @Test
    void findByKullaniciIdOlusturulmaDescSiradaDoner() {
        Long id = kullaniciOlustur("mehmet@ornek.com");
        OffsetDateTime simdi = OffsetDateTime.now();
        rotaEkle(id, "En Eski", simdi.minusDays(2));
        rotaEkle(id, "Ortanca", simdi.minusDays(1));
        rotaEkle(id, "En Yeni", simdi);

        List<KayitliRota> sonuc = kayitliRotaRepository.findByKullaniciIdOrderByOlusturulmaDesc(id);

        assertThat(sonuc).extracting(KayitliRota::getBaslik)
                .containsExactly("En Yeni", "Ortanca", "En Eski");
    }
}
