package com.seyyah.route;

import com.seyyah.place.KoridorYeri;

import java.util.List;
import java.util.Map;

public record RotaYaniti(List<Rota> rotalar) {

    public record Rota(
            int sira,
            String ad,
            String uzerinden,
            double mesafeM,
            double sureSn,
            List<List<Double>> geometri,
            Map<String, List<KoridorYeri>> yerler
    ) {
    }
}
