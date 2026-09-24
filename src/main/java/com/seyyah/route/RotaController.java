package com.seyyah.route;

import com.seyyah.place.KoridorYeri;
import com.seyyah.place.PlaceRepository;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api/rota")
public class RotaController {

    private static final Logger log = LoggerFactory.getLogger(RotaController.class);

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

        long t0 = System.nanoTime();
        RotaSonucu ana = openRouteService.getRoute(kalkisBoylam, kalkisEnlem, varisBoylam, varisEnlem);
        long t1 = System.nanoTime();

        List<AlternatifRotaServisi.RotaSecenegi> alternatifler = alternatifRotaServisi.alternatifleriBul(
                ana, kalkisBoylam, kalkisEnlem, varisBoylam, varisEnlem);
        long t2 = System.nanoTime();

        List<RotaTanimi> tanimlar = new ArrayList<>();
        tanimlar.add(new RotaTanimi("En hızlı", null, null, ana));
        alternatifler.forEach(a -> tanimlar.add(new RotaTanimi(a.ad(), a.uzerinden(), a.araNokta(), a.sonuc())));

        List<RotaYaniti.Rota> rotalar = rotalariOlustur(tanimlar, yaricap, limit);
        // Uzun rotalarda yanıt süresi hangi aşamada harcanıyor, canlıda da görülebilsin
        log.info("Rota {} km, {} alternatif: ana {} ms, alternatif {} ms, yerler {} ms",
                Math.round(ana.mesafeM() / 1000), alternatifler.size(),
                (t1 - t0) / 1_000_000, (t2 - t1) / 1_000_000, (System.nanoTime() - t2) / 1_000_000);
        return new RotaYaniti(rotalar);
    }

    private record RotaTanimi(String ad, String uzerinden, AraNokta araNokta, RotaSonucu sonuc) {
    }

    private static final List<String> TURLER = List.of("gezi", "mola", "destek");

    // Rota başına 3, toplam en fazla 9 koridor sorgusu; uzun rotada her biri ~0,5 sn sürdüğünden
    // sırayla çalışınca yanıt 10 sn'yi buluyordu. Eşzamanlılığı bağlantı havuzu (5) zaten sınırlar.
    private List<RotaYaniti.Rota> rotalariOlustur(List<RotaTanimi> tanimlar, int yaricap, int limit) {
        try (ExecutorService yurutucu = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Map<String, Future<List<KoridorYeri>>>> sorgular = new ArrayList<>();
            for (RotaTanimi tanim : tanimlar) {
                Map<String, Future<List<KoridorYeri>>> turSorgulari = new LinkedHashMap<>();
                for (String tur : TURLER) {
                    turSorgulari.put(tur, yurutucu.submit(
                            () -> placeRepository.koridorda(tanim.sonuc().wkt(), tur, yaricap, limit)));
                }
                sorgular.add(turSorgulari);
            }

            List<RotaYaniti.Rota> rotalar = new ArrayList<>();
            for (int sira = 0; sira < tanimlar.size(); sira++) {
                RotaTanimi tanim = tanimlar.get(sira);
                Map<String, List<KoridorYeri>> yerler = new LinkedHashMap<>();
                sorgular.get(sira).forEach((tur, sorgu) -> yerler.put(tur, sonucuAl(sorgu)));
                rotalar.add(new RotaYaniti.Rota(sira, tanim.ad(), tanim.uzerinden(), tanim.araNokta(), tanim.sonuc().mesafeM(),
                        tanim.sonuc().sureSn(), CizgiSadelestirici.sadelestir(tanim.sonuc().koordinatlar(), CizgiSadelestirici.VARSAYILAN_TOLERANS), yerler));
            }
            return rotalar;
        }
    }

    private static <T> T sonucuAl(Future<T> sorgu) {
        try {
            return sorgu.get();
        } catch (ExecutionException e) {
            // Sorgunun kendi hatası (ör. DataAccessException) olduğu gibi yukarı çıksın
            if (e.getCause() instanceof RuntimeException r) throw r;
            throw new IllegalStateException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @PostMapping("/duraklu")
    public DurakliRotaYaniti durakliRota(@Valid @RequestBody DurakliRotaIstegi istek) {
        List<List<Double>> koordinatlar = istek.noktalar().stream()
                .map(n -> List.of(n.boylam(), n.enlem()))
                .toList();

        RotaSonucu sonuc = openRouteService.getRoute(koordinatlar);

        return new DurakliRotaYaniti(
                sonuc.mesafeM(),
                sonuc.sureSn(),
                CizgiSadelestirici.sadelestir(sonuc.koordinatlar(), CizgiSadelestirici.VARSAYILAN_TOLERANS),
                sonuc.bacaklar()
        );
    }
}
