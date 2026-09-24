package com.seyyah.detay;

public record YerDetay(
        Long id,
        String ad,
        String kategori,
        String tur,
        Double enlem,
        Double boylam,
        String ucret,
        String calismaSaatleri,
        String website,
        WikidataDetay wikidata,
        GoogleDetay google
) {}
