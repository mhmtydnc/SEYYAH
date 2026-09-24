package com.seyyah.route;

import java.util.List;

public record DurakliRotaYaniti(
        double mesafeM,
        double sureSn,
        List<List<Double>> geometri,
        List<RotaSonucu.Bacak> bacaklar
) {
}
