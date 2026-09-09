package com.seyyah.place;

import org.springframework.web.bind.annotation.*;
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

    public List<Map<String, Object>> koridor(
            @RequestParam String wkt,
            @RequestParam(defaultValue = "gezi") String tur,
            @RequestParam(defaultValue="5000") double yaricap,
            @RequestParam(defaultValue="40") int limit)
    {

        return repo.koridorda(wkt,tur, yaricap,limit).stream()
                .map(r->Map.of(
                        "id",r[0],
                        "ad",r[1],
                        "kategori", r[2],
                        "enlem", r[4] ,
                        "boylam", r[5],
                        "yolaUzaklikM", Math.round(((Number)r[6]).doubleValue()),
                        "yolOrani", r[7])).toList();
    }

}
