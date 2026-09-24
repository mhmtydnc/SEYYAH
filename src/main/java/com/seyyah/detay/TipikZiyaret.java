package com.seyyah.detay;

import java.util.Map;

// Kategoriye göre tipik ziyaret süresi (dakika). Arayüzdeki varsayılan kalış süreleriyle
// (frontend/src/rota/kalisSureleri.ts) aynı tutulmalı; biri değişirse diğeri de değişir.
final class TipikZiyaret {

    private static final Map<String, Integer> SURELER = Map.ofEntries(
            Map.entry("museum", 90), Map.entry("archaeological_site", 90), Map.entry("castle", 60),
            Map.entry("ruins", 45), Map.entry("attraction", 60), Map.entry("viewpoint", 20),
            Map.entry("waterfall", 40), Map.entry("beach", 120), Map.entry("park", 30),
            Map.entry("nature_reserve", 90), Map.entry("garden", 30), Map.entry("place_of_worship", 20),
            Map.entry("monument", 15), Map.entry("memorial", 15), Map.entry("artwork", 10),
            Map.entry("restaurant", 60), Map.entry("cafe", 30), Map.entry("fuel", 10),
            Map.entry("toilets", 5), Map.entry("parking", 5));

    private TipikZiyaret() {
    }

    static int dakika(String kategori) {
        return kategori == null ? 30 : SURELER.getOrDefault(kategori, 30);
    }
}
