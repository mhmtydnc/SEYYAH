package com.seyyah.konum;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

@RestClientTest(value = KonumServisi.class,
        properties = {"ors.api.url=https://ors.test", "ors.api.key=test-anahtar"})
class KonumServisiTest {

    @Autowired
    private KonumServisi servis;

    @Autowired
    private MockRestServiceServer sunucu;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private JdbcClient jdbcClient;

    @Test
    void kendiVeriDoluysaOrsCagrilmaz() {
        when(jdbcClient.sql(anyString())
                .param(anyString(), any())
                .param(anyString(), any())
                .param(anyString(), any())
                .query(KonumSonucu.class)
                .list())
                .thenReturn(List.of(new KonumSonucu("Ankara", "Ankara, Türkiye", 39.9, 32.8)));

        List<KonumSonucu> sonuclar = servis.ara("Ankara", 5);

        assertThat(sonuclar).hasSize(1);
        assertThat(sonuclar.get(0).ad()).isEqualTo("Ankara");
        // Beklenmeyen bir ORS isteği gelirse MockRestServiceServer hata verir
        sunucu.verify();
    }

    @Test
    void kendiVeriBossaOrsCagrilir() {
        when(jdbcClient.sql(anyString())
                .param(anyString(), any())
                .param(anyString(), any())
                .param(anyString(), any())
                .query(KonumSonucu.class)
                .list())
                .thenReturn(List.of());

        sunucu.expect(requestTo("https://ors.test/geocode/autocomplete?text=Kapadokya&size=5&boundary.country=TR"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"features":[{"properties":{"name":"Göreme","label":"Göreme, Nevşehir, Türkiye"},"geometry":{"coordinates":[34.83,38.64]}}]}
                        """, MediaType.APPLICATION_JSON));

        List<KonumSonucu> sonuclar = servis.ara("Kapadokya", 5);

        assertThat(sonuclar).hasSize(1);
        assertThat(sonuclar.get(0).ad()).isEqualTo("Göreme");
        sunucu.verify();
    }

    @Test
    void orsHataVerirseBosListeDoner() {
        when(jdbcClient.sql(anyString())
                .param(anyString(), any())
                .param(anyString(), any())
                .param(anyString(), any())
                .query(KonumSonucu.class)
                .list())
                .thenReturn(List.of());

        sunucu.expect(requestTo("https://ors.test/geocode/autocomplete?text=HataYer&size=5&boundary.country=TR"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        List<KonumSonucu> sonuclar = servis.ara("HataYer", 5);

        assertThat(sonuclar).isEmpty();
        sunucu.verify();
    }

    @Test
    void sadelestirmeVeritabanindakiUnaccentIleAyniSonucuVerir() {
        assertThat(KonumServisi.sadelestir("  İSTANBUL ")).isEqualTo("istanbul");
        assertThat(KonumServisi.sadelestir("Iğdır")).isEqualTo("igdir");
        assertThat(KonumServisi.sadelestir("Şişli Çağlayan Göreme Ürgüp")).isEqualTo("sisli caglayan goreme urgup");
    }
}
