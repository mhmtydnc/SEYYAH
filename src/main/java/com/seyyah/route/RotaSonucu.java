package com.seyyah.route;

import java.util.List;

public record RotaSonucu(
        String wkt,
        List<List<Double>> koordinatlar,
        double mesafeM,
        double sureSn
) {
}
