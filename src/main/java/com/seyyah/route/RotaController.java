package com.seyyah.route;

import com.seyyah.place.KoridorYeri;
import com.seyyah.place.PlaceRepository;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rota")
public class RotaController {

    private final OpenRouteService openRouteService;
    private final AlternatifRotaServisi alternatifRotaServisi;
    private final PlaceRepository placeRepository;

    public RotaController(OpenRouteService openRouteService, AlternatifRotaServisi alternatifRotaServisi,
                           PlaceRepository placeRepository) {
        this.openRouteService = openRouteService;
        this.alternatifRotaServisi = alternatifRotaServisi;
        this.placeRepository = placeRepository;
    }

    @GetMapping
    public RotaYaniti rota(
            @RequestParam("kalkisEnlem") @DecimalMin("-90") @DecimalMax("90") double kalkisEnlem,
            @RequestParam("kalkisBoylam") @DecimalMin("-180") @DecimalMax("180") double kalkisBoylam,
            @RequestParam("varisEnlem") @DecimalMin("-90") @DecimalMax("90") double varisEnlem,
            @RequestParam("varisBoylam") @DecimalMin("-180") @DecimalMax("180") double varisBoylam,
            @RequestParam(defaultValue = "5000") @Min(100) @Max(20000) int yaricap,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {

        RotaSonucu ana = openRouteService.getRoute(kalkisBoylam, kalkisEnlem, varisBoylam, varisEnlem);

        List<AlternatifRotaServisi.RotaSecenegi> alternatifler = alternatifRotaServisi.alternatifleriBul(
                ana, kalkisBoylam, kalkisEnlem, varisBoylam, varisEnlem);

        List<RotaYaniti.Rota> rotalar = new ArrayList<>();
        rotalar.add(rotaOlustur(0, "En hızlı", null, ana, yaricap, limit));

        int sira = 1;
        for (AlternatifRotaServisi.RotaSecenegi alternatif : alternatifler) {
            rotalar.add(rotaOlustur(sira, alternatif.ad(), alternatif.uzerinden(), alternatif.sonuc(), yaricap, limit));
            sira++;
        }

        return new RotaYaniti(rotalar);
    }

    private RotaYaniti.Rota rotaOlustur(int sira, String ad, String uzerinden, RotaSonucu sonuc,
                                         int yaricap, int limit) {
        List<KoridorYeri> gezi = placeRepository.koridorda(sonuc.wkt(), "gezi", yaricap, limit);
        List<KoridorYeri> mola = placeRepository.koridorda(sonuc.wkt(), "mola", yaricap, limit);
        List<KoridorYeri> destek = placeRepository.koridorda(sonuc.wkt(), "destek", yaricap, limit);

        return new RotaYaniti.Rota(
                sira,
                ad,
                uzerinden,
                sonuc.mesafeM(),
                sonuc.sureSn(),
                sonuc.koordinatlar(),
                Map.of(
                        "gezi", gezi,
                        "mola", mola,
                        "destek", destek
                )
        );
    }
}
