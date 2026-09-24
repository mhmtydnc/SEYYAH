package com.seyyah.place;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// POST /api/places/koridor gövdesi. Gönderilmeyen alanlar GET'teki varsayılanları alır.
public record KoridorIstegi(
        @NotBlank String wkt,
        @Pattern(regexp = "gezi|mola|destek") String tur,
        @Min(100) @Max(20000) Integer yaricap,
        @Min(1) @Max(200) Integer limit) {

    public KoridorIstegi {
        if (tur == null) tur = "gezi";
        if (yaricap == null) yaricap = 5000;
        if (limit == null) limit = 40;
    }
}
