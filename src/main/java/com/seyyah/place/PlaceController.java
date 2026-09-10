package com.seyyah.place;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/places")
public class PlaceController {
    private final PlaceRepository repo;
    public PlaceController(PlaceRepository repo) {
        this.repo = repo;
    }
    @GetMapping("/koridor")

    public List<KoridorYeri> koridor(
            @RequestParam String wkt,
            @RequestParam(defaultValue = "gezi") @Pattern(regexp = "gezi|mola") String tur,
            @RequestParam(defaultValue="5000") @Min(100) @Max(20000) int yaricap,
            @RequestParam(defaultValue="40") @Min(1) @Max(200) int limit)
    {
        return repo.koridorda(wkt, tur, yaricap, limit);
    }

}
