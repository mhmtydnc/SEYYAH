package com.seyyah.route;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(value = OpenRouteService.class,
        properties = {"ors.api.url=https://ors.test", "ors.api.key=test-anahtar"})
class OpenRouteServiceTest {

    private static final String URL = "https://ors.test/v2/directions/driving-car/geojson";

    @Autowired
    private OpenRouteService servis;

    @Autowired
    private MockRestServiceServer sunucu;

    @Test
    void geojsonRotayiWktyeCevirir() {
        sunucu.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "test-anahtar"))
                .andExpect(jsonPath("$.radiuses[0]").value(5000))
                .andExpect(jsonPath("$.radiuses[1]").value(5000))
                .andRespond(withSuccess("""
                        {"type":"FeatureCollection","features":[{"properties":{"summary":{"distance":120.5,"duration":300}},"geometry":{"type":"LineString",
                         "coordinates":[[29.0,41.0],[29.5,40.5],[32,39.9]]}}]}
                        """, MediaType.APPLICATION_JSON));

        RotaSonucu sonuc = servis.getRoute(29.0, 41.0, 32.0, 39.9);
        assertThat(sonuc.wkt()).isEqualTo("SRID=4326;LINESTRING(29.0 41.0, 29.5 40.5, 32.0 39.9)");
        assertThat(sonuc.mesafeM()).isEqualTo(120.5);
        assertThat(sonuc.sureSn()).isEqualTo(300);
        assertThat(sonuc.koordinatlar()).hasSize(3);
    }

    @Test
    void alternatifRotalarIstektaAlternativeRoutesGovdesiTasir() {
        sunucu.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.alternative_routes.target_count").value(3))
                .andExpect(jsonPath("$.alternative_routes.share_factor").value(0.6))
                .andExpect(jsonPath("$.alternative_routes.weight_factor").value(1.6))
                .andRespond(withSuccess("""
                        {"type":"FeatureCollection","features":[
                          {"properties":{"summary":{"distance":83000,"duration":3600}},"geometry":{"type":"LineString",
                           "coordinates":[[27.1,38.4],[27.3,38.0]]}},
                          {"properties":{"summary":{"distance":90000,"duration":4000}},"geometry":{"type":"LineString",
                           "coordinates":[[27.1,38.4],[27.5,38.1]]}}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        var sonuclar = servis.getAlternativeRoutes(27.1, 38.4, 27.4, 37.9);

        assertThat(sonuclar).hasSize(2);
        assertThat(sonuclar.get(0).mesafeM()).isEqualTo(83000);
        assertThat(sonuclar.get(1).mesafeM()).isEqualTo(90000);
        assertThat(sonuclar.get(1).sureSn()).isEqualTo(4000);
    }

    @Test
    void bosFeatureListesiRotaBulunamadiSayilir() {
        sunucu.expect(requestTo(URL))
                .andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> servis.getRouteWkt(29, 41, 32, 39))
                .isInstanceOf(RotaServisiException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void ors404RotaBulunamadiOlur() {
        orsHatasi(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND);
    }

    @Test
    void orsAnahtarHatasiBadGatewayOlur() {
        orsHatasi(HttpStatus.FORBIDDEN, HttpStatus.BAD_GATEWAY);
    }

    @Test
    void orsKotasiDolunca503Olur() {
        orsHatasi(HttpStatus.TOO_MANY_REQUESTS, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void ors5xxBadGatewayOlur() {
        orsHatasi(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY);
    }

    private void orsHatasi(HttpStatus orsDurumu, HttpStatus beklenen) {
        sunucu.expect(requestTo(URL))
                .andRespond(withStatus(orsDurumu)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":2009,\"message\":\"test\"}}"));

        assertThatThrownBy(() -> servis.getRouteWkt(29, 41, 32, 39))
                .isInstanceOf(RotaServisiException.class)
                .extracting("status").isEqualTo(beklenen);
    }
}
