package com.seyyah.rotalarim;

import com.seyyah.route.AraNokta;

import java.time.OffsetDateTime;
import java.util.List;

public record RotaYaniti(
        Long id,
        String baslik,
        Konum kalkis,
        Konum varis,
        AraNokta uzerinden,
        List<Durak> duraklar,
        OffsetDateTime olusturulma) {

    public static RotaYaniti olustur(KayitliRota rota) {
        return new RotaYaniti(
                rota.getId(),
                rota.getBaslik(),
                new Konum(rota.getKalkisAd(), rota.getKalkisEnlem(), rota.getKalkisBoylam()),
                new Konum(rota.getVarisAd(), rota.getVarisEnlem(), rota.getVarisBoylam()),
                rota.getUzerinden(),
                rota.getDuraklar() != null ? rota.getDuraklar() : List.of(),
                rota.getOlusturulma());
    }
}
