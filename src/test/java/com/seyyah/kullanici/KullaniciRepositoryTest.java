package com.seyyah.kullanici;

import com.seyyah.PostgisTestDestegi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KullaniciRepositoryTest extends PostgisTestDestegi {

    @Autowired
    private KullaniciRepository kullaniciRepository;

    private Kullanici olustur(String eposta) {
        Kullanici k = new Kullanici();
        k.setAd("Mehmet");
        k.setEposta(eposta);
        k.setSifreOzeti("hash");
        return k;
    }

    @Test
    void ayniEpostaIleIkinciKayitReddedilir() {
        kullaniciRepository.saveAndFlush(olustur("mehmet@ornek.com"));

        assertThatThrownBy(() -> kullaniciRepository.saveAndFlush(olustur("mehmet@ornek.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
