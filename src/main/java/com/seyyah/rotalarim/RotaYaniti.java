package com.seyyah.rotalarim;

import java.time.OffsetDateTime;

public record RotaYaniti(Long id, String baslik, Konum kalkis, Konum varis, OffsetDateTime olusturulma) {

    public static RotaYaniti olustur(KayitliRota rota) {
        return new RotaYaniti(
                rota.getId(),
                rota.getBaslik(),
                new Konum(rota.getKalkisAd(), rota.getKalkisEnlem(), rota.getKalkisBoylam()),
                new Konum(rota.getVarisAd(), rota.getVarisEnlem(), rota.getVarisBoylam()),
                rota.getOlusturulma());
    }
}
