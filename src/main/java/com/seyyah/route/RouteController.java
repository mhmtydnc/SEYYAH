package com.seyyah.route;

import com.seyyah.place.KoridorYeri;
import com.seyyah.place.PlaceRepository;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final OpenRouteService openRouteService;
    private final PlaceRepository placeRepository;

    public RouteController(OpenRouteService openRouteService, PlaceRepository placeRepository) {
        this.openRouteService = openRouteService;
        this.placeRepository = placeRepository;
    }

    @GetMapping("/routeStreet")
    public List<KoridorYeri> getRouteCorridors(
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double startLon,
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double startLat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double endLon,
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double endLat,
            @RequestParam(defaultValue = "gezi") @Pattern(regexp = "gezi|mola|destek") String tur,
            @RequestParam(defaultValue = "5000") @Min(100) @Max(20000) int yaricap,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int limit) {

        String wkt = openRouteService.getRouteWkt(startLon, startLat, endLon, endLat);
        return placeRepository.koridorda(wkt, tur, yaricap, limit);
    }
}
