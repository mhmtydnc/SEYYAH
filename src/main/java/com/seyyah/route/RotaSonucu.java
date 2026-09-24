package com.seyyah.route;

import java.util.List;

public record RotaSonucu(
        String wkt,
        List<List<Double>> koordinatlar,
        double mesafeM,
        double sureSn,
        List<Bacak> bacaklar
) {
    public RotaSonucu(String wkt, List<List<Double>> koordinatlar, double mesafeM, double sureSn) {
        this(wkt, koordinatlar, mesafeM, sureSn, List.of());
    }

    public record Bacak(double mesafeM, double sureSn) {}
}
