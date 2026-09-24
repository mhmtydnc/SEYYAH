package com.seyyah.route;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// DB gerektirmez: JdbcClient mock'lanır ama hiç çağrılmaz, SQL adımları spy ile stub'lanır
class AlternatifRotaServisiTest {

    private static final double K_LON = 32.85, K_LAT = 39.92;
    private static final double V_LON = 27.35, V_LAT = 38.32;

    private final OpenRouteService openRouteService = mock(OpenRouteService.class);
    private final JdbcClient jdbcClient = mock(JdbcClient.class);
    private final AlternatifRotaServisi servisi = spy(new AlternatifRotaServisi(openRouteService, jdbcClient));

    @Test
    void kisaMesafedeOrsAlternatifleriKullanilir() {
        RotaSonucu ana = new RotaSonucu("ana-wkt", List.of(), 83_000, 3600);
        RotaSonucu alt1 = new RotaSonucu("alt1-wkt", List.of(), 90_000, 4200);
        RotaSonucu alt2 = new RotaSonucu("alt2-wkt", List.of(), 95_000, 4000);
        given(openRouteService.getAlternativeRoutes(K_LON, K_LAT, V_LON, V_LAT))
                .willReturn(List.of(ana, alt1, alt2));

        List<AlternatifRotaServisi.RotaSecenegi> sonuc = servisi.alternatifleriBul(ana, K_LON, K_LAT, V_LON, V_LAT);

        assertThat(sonuc).hasSize(2);
        // süreye göre artan: alt2 (4000) < alt1 (4200); numara sıralamadan sonra verilir
        assertThat(sonuc.get(0).sonuc()).isEqualTo(alt2);
        assertThat(sonuc.get(0).ad()).isEqualTo("Alternatif 1");
        assertThat(sonuc.get(0).uzerinden()).isNull();
        assertThat(sonuc.get(1).sonuc()).isEqualTo(alt1);
        assertThat(sonuc.get(1).ad()).isEqualTo("Alternatif 2");

        verify(jdbcClient, never()).sql(anyString());
    }

    @Test
    void kisaMesafedeTekFeatureVarsaAlternatifYok() {
        RotaSonucu ana = new RotaSonucu("ana-wkt", List.of(), 83_000, 3600);
        given(openRouteService.getAlternativeRoutes(K_LON, K_LAT, V_LON, V_LAT)).willReturn(List.of(ana));

        assertThat(servisi.alternatifleriBul(ana, K_LON, K_LAT, V_LON, V_LAT)).isEmpty();
    }

    @Test
    void kisaMesafedeOrsHatasindaBosListeDoner() {
        RotaSonucu ana = new RotaSonucu("ana-wkt", List.of(), 83_000, 3600);
        willThrow(new RotaServisiException(org.springframework.http.HttpStatus.BAD_REQUEST, "hata"))
                .given(openRouteService).getAlternativeRoutes(K_LON, K_LAT, V_LON, V_LAT);

        assertThat(servisi.alternatifleriBul(ana, K_LON, K_LAT, V_LON, V_LAT)).isEmpty();
    }

    @Test
    void uzunMesafedeAdayDenenirVeKabulEdilenlerSureyeGoreSiralanir() {
        RotaSonucu ana = new RotaSonucu("ana-wkt", List.of(), 300_000, 12000);
        AdayKonum aksaray = new AdayKonum("Aksaray", 38.37, 34.03);
        AdayKonum nevsehir = new AdayKonum("Nevşehir", 38.62, 34.72);
        doReturn(List.of(aksaray, nevsehir))
                .when(servisi).adaySehirleriBul(eq(ana), anyDouble(), anyDouble(), anyDouble(), anyDouble());

        RotaSonucu aksarayRota = new RotaSonucu("aksaray-wkt", List.of(), 320_000, 13000);
        RotaSonucu nevsehirRota = new RotaSonucu("nevsehir-wkt", List.of(), 315_000, 12500);
        given(openRouteService.getRouteViaPoint(K_LON, K_LAT, aksaray.boylam(), aksaray.enlem(), V_LON, V_LAT))
                .willReturn(aksarayRota);
        given(openRouteService.getRouteViaPoint(K_LON, K_LAT, nevsehir.boylam(), nevsehir.enlem(), V_LON, V_LAT))
                .willReturn(nevsehirRota);

        // ikisi de örtüşme eşiğinin altında kabul edilir
        doReturn(0.1).when(servisi).ortusmeOrani(anyString(), anyString());

        List<AlternatifRotaServisi.RotaSecenegi> sonuc = servisi.alternatifleriBul(ana, K_LON, K_LAT, V_LON, V_LAT);

        assertThat(sonuc).hasSize(2);
        // süreye göre artan: nevşehir (12500) < aksaray (13000)
        assertThat(sonuc.get(0).ad()).isEqualTo("Nevşehir üzerinden");
        assertThat(sonuc.get(0).uzerinden()).isEqualTo("Nevşehir");
        assertThat(sonuc.get(1).ad()).isEqualTo("Aksaray üzerinden");
        assertThat(sonuc.get(1).uzerinden()).isEqualTo("Aksaray");
    }

    @Test
    void uzunMesafedeSureAsanAdayElenir() {
        RotaSonucu ana = new RotaSonucu("ana-wkt", List.of(), 300_000, 10_000);
        AdayKonum aday = new AdayKonum("Aksaray", 38.37, 34.03);
        doReturn(List.of(aday))
                .when(servisi).adaySehirleriBul(eq(ana), anyDouble(), anyDouble(), anyDouble(), anyDouble());

        // 1.5 kat sınırın (15000) üstünde süre -> elenir
        RotaSonucu adayRota = new RotaSonucu("aday-wkt", List.of(), 320_000, 15_001);
        given(openRouteService.getRouteViaPoint(K_LON, K_LAT, aday.boylam(), aday.enlem(), V_LON, V_LAT))
                .willReturn(adayRota);

        List<AlternatifRotaServisi.RotaSecenegi> sonuc = servisi.alternatifleriBul(ana, K_LON, K_LAT, V_LON, V_LAT);

        assertThat(sonuc).isEmpty();
        verify(servisi, never()).ortusmeOrani(anyString(), anyString());
    }

    @Test
    void uzunMesafedeYuksekOrtusmeAdayElenir() {
        RotaSonucu ana = new RotaSonucu("ana-wkt", List.of(), 300_000, 10_000);
        AdayKonum aday = new AdayKonum("Aksaray", 38.37, 34.03);
        doReturn(List.of(aday))
                .when(servisi).adaySehirleriBul(eq(ana), anyDouble(), anyDouble(), anyDouble(), anyDouble());

        RotaSonucu adayRota = new RotaSonucu("aday-wkt", List.of(), 320_000, 11_000);
        given(openRouteService.getRouteViaPoint(K_LON, K_LAT, aday.boylam(), aday.enlem(), V_LON, V_LAT))
                .willReturn(adayRota);
        // ana rotayla örtüşme eşiği (0.6) aşılıyor -> elenir
        doReturn(0.75).when(servisi).ortusmeOrani("aday-wkt", "ana-wkt");

        assertThat(servisi.alternatifleriBul(ana, K_LON, K_LAT, V_LON, V_LAT)).isEmpty();
    }

    @Test
    void dortOrsCagrisiSinirindaDurur() {
        RotaSonucu ana = new RotaSonucu("ana-wkt", List.of(), 300_000, 10_000);
        List<AdayKonum> adaylar = List.of(
                new AdayKonum("Aday1", 38.1, 34.1),
                new AdayKonum("Aday2", 38.2, 34.2),
                new AdayKonum("Aday3", 38.3, 34.3),
                new AdayKonum("Aday4", 38.4, 34.4),
                new AdayKonum("Aday5", 38.5, 34.5),
                new AdayKonum("Aday6", 38.6, 34.6)
        );
        doReturn(adaylar)
                .when(servisi).adaySehirleriBul(eq(ana), anyDouble(), anyDouble(), anyDouble(), anyDouble());

        // her aday süre sınırını aşıp elenir, hiçbiri kabul edilmez
        given(openRouteService.getRouteViaPoint(
                Mockito.eq(K_LON), Mockito.eq(K_LAT), Mockito.anyDouble(), Mockito.anyDouble(),
                Mockito.eq(V_LON), Mockito.eq(V_LAT)))
                .willReturn(new RotaSonucu("reddedilen-wkt", List.of(), 400_000, 20_000));

        List<AlternatifRotaServisi.RotaSecenegi> sonuc = servisi.alternatifleriBul(ana, K_LON, K_LAT, V_LON, V_LAT);

        assertThat(sonuc).isEmpty();
        // 6 aday olmasına rağmen en fazla 4 ORS çağrısı yapılır
        verify(openRouteService, times(4)).getRouteViaPoint(
                Mockito.eq(K_LON), Mockito.eq(K_LAT), Mockito.anyDouble(), Mockito.anyDouble(),
                Mockito.eq(V_LON), Mockito.eq(V_LAT));
    }

    @Test
    void uzunMesafedeAdaySorgusuHataVerirseBosListeDoner() {
        RotaSonucu ana = new RotaSonucu("ana-wkt", List.of(), 300_000, 10_000);
        willThrow(new RuntimeException("sql hatası"))
                .given(servisi).adaySehirleriBul(eq(ana), anyDouble(), anyDouble(), anyDouble(), anyDouble());

        assertThat(servisi.alternatifleriBul(ana, K_LON, K_LAT, V_LON, V_LAT)).isEmpty();
    }
}
