package com.seyyah.place;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;

@RestController
@RequestMapping("/api/places")
public class PlaceController {
    private final PlaceRepository repo;
    public PlaceController(PlaceRepository repo) {
        this.repo = repo;
    }

    // Kısa WKT'lerle elle denemek için. Gerçek rotalar URL sınırını aşar → POST kullanılmalı.
    @GetMapping("/koridor")
    public List<KoridorYeri> koridor(
            @RequestParam String wkt,
            @RequestParam(defaultValue = "gezi") @Pattern(regexp = "gezi|mola|destek") String tur,
            @RequestParam(defaultValue="5000") @Min(100) @Max(20000) int yaricap,
            @RequestParam(defaultValue="40") @Min(1) @Max(200) int limit)
    {
        return repo.koridorda(wkt, tur, yaricap, limit);
    }

    @PostMapping("/koridor")
    public List<KoridorYeri> koridor(@Valid @RequestBody KoridorIstegi istek) {
        return repo.koridorda(istek.wkt(), istek.tur(), istek.yaricap(), istek.limit());
    }

}
