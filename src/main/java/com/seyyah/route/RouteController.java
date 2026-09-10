package com.seyyah.route;

import com.seyyah.place.KoridorYeri;
import com.seyyah.place.PlaceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
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
    public ResponseEntity<List<List<KoridorYeri>>> getRouteCorridors(
            @RequestParam double startLon, @RequestParam double startLat,
            @RequestParam double endLon, @RequestParam double endLat) {

        // OpenRouteService ile alternatif rotaların WKT çizgilerini al
        List<String> routeWkts = openRouteService.getRouteWkt(startLon, startLat, endLon, endLat);

        List<List<KoridorYeri>> allRoutePlaces = new ArrayList<>();

        // Her rota çizgisi için veritabanındaki koridorda() metodunu çalıştır
        for (String wkt : routeWkts) {
            List<KoridorYeri> placesOnRoute = placeRepository.koridorda(wkt, "gezi", 5000, 20);
            allRoutePlaces.add(placesOnRoute);
        }

        return ResponseEntity.ok(allRoutePlaces);
    }
}