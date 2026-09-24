package com.seyyah.route;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CizgiSadelestiriciTest {

    @Test
    void duzCizgidekiAraNoktalarAtilirUclarKalir() {
        List<List<Double>> duz = new ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            duz.add(List.of(29.0 + i * 0.001, 41.0));
        }

        List<List<Double>> sonuc = CizgiSadelestirici.sadelestir(duz, CizgiSadelestirici.VARSAYILAN_TOLERANS);

        assertThat(sonuc).containsExactly(duz.get(0), duz.get(100));
    }

    @Test
    void toleranstanBuyukKirilmaKorunur() {
        List<List<Double>> kirik = List.of(List.of(29.0, 41.0), List.of(29.5, 41.3), List.of(30.0, 41.0));

        assertThat(CizgiSadelestirici.sadelestir(kirik, CizgiSadelestirici.VARSAYILAN_TOLERANS)).isEqualTo(kirik);
    }

    @Test
    void ucNoktadanAzsaOldugugibiDoner() {
        List<List<Double>> iki = List.of(List.of(29.0, 41.0), List.of(30.0, 41.0));

        assertThat(CizgiSadelestirici.sadelestir(iki, 1.0)).isSameAs(iki);
    }
}
