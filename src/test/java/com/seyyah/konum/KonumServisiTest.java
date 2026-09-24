package com.seyyah.konum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(value = KonumServisi.class,
        properties = {"ors.api.url=https://ors.test", "ors.api.key=test-anahtar"})
class KonumServisiTest {

    @Autowired
    private KonumServisi servis;

    @Autowired
    private MockRestServiceServer sunucu;

    @Test
    void geojsonAramaSonucunuCevirir() {
        sunucu.expect(requestTo("https://ors.test/geocode/autocomplete?text=Kapadokya&size=5&boundary.country=TR"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"features":[{"properties":{"name":"Göreme","label":"Göreme, Nevşehir, Türkiye"},"geometry":{"coordinates":[34.83,38.64]}}]}
                        """, MediaType.APPLICATION_JSON));

        List<KonumSonucu> sonuclar = servis.ara("Kapadokya", 5);
        assertThat(sonuclar).hasSize(1);
        assertThat(sonuclar.get(0).ad()).isEqualTo("Göreme");
        assertThat(sonuclar.get(0).etiket()).isEqualTo("Göreme, Nevşehir, Türkiye");
        assertThat(sonuclar.get(0).enlem()).isEqualTo(38.64);
        assertThat(sonuclar.get(0).boylam()).isEqualTo(34.83);
    }
}
