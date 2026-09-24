package com.seyyah.route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Ana rotaya ek olarak en fazla 2 alternatif üretir. ORS public API'de alternative_routes
// yalnızca yaklaşık ~100 km'e kadar çalıştığından (üstünde 400/2004 döner) iki farklı strateji var:
// kısa mesafede ORS'un kendi alternatifleri, uzun mesafede ara şehir üzerinden zincirleme deneme.
@Service
public class AlternatifRotaServisi {

    private static final Logger log = LoggerFactory.getLogger(AlternatifRotaServisi.class);

    // ORS'un alternative_routes'u ~100 km yaklaşık mesafeye kadar çalışıyor; pay bırakmak için 95 km sınır
    private static final double ANA_ROTA_ESIK_M = 95_000;
    private static final int HEDEF_ALTERNATIF_SAYISI = 2;
    private static final int MAKS_ADAY_SEHIR = 6;
    // Ara şehir denemesinde kota korumak için toplam ORS çağrısı sınırı
    private static final int MAKS_ORS_CAGRISI = 4;
    private static final double SURE_CARPANI = 1.5;
    private static final double ORTUSME_ESIGI = 0.6;

    private final OpenRouteService openRouteService;
    private final JdbcClient jdbcClient;

    public AlternatifRotaServisi(OpenRouteService openRouteService, JdbcClient jdbcClient) {
        this.openRouteService = openRouteService;
        this.jdbcClient = jdbcClient;
    }

    // Alternatif hesaplamada oluşan HERHANGİ bir hata (ORS, SQL, zaman aşımı) ana rotayı bozmasın;
    // sadece loglanır ve boş liste dönülür.
    public List<RotaSecenegi> alternatifleriBul(RotaSonucu ana, double kalkisLon, double kalkisLat,
                                                 double varisLon, double varisLat) {
        try {
            List<RotaSecenegi> sonuc = ana.mesafeM() <= ANA_ROTA_ESIK_M
                    ? orsAlternatifleriIleBul(kalkisLon, kalkisLat, varisLon, varisLat)
                    : araSehirlerIleBul(ana, kalkisLon, kalkisLat, varisLon, varisLat);

            List<RotaSecenegi> sirali = sonuc.stream()
                    .sorted(Comparator.comparingDouble(rs -> rs.sonuc().sureSn()))
                    .toList();
            // ORS alternatiflerinin numarası sıralamadan sonra verilir, yoksa "Alternatif 2" önce görünür
            List<RotaSecenegi> adlandirilmis = new ArrayList<>();
            int no = 1;
            for (RotaSecenegi rs : sirali) {
                adlandirilmis.add(rs.uzerinden() != null ? rs : new RotaSecenegi(rs.sonuc(), "Alternatif " + no++, null));
            }
            return adlandirilmis;
        } catch (Exception e) {
            log.warn("Alternatif rota hesaplanamadı, yalnızca ana rota dönülüyor: {}", e.getMessage());
            return List.of();
        }
    }

    // Kısa mesafe: ORS'un kendi alternative_routes'u tek istekte ana rotayı da döndürür (ilk feature)
    private List<RotaSecenegi> orsAlternatifleriIleBul(double kalkisLon, double kalkisLat,
                                                         double varisLon, double varisLat) {
        List<RotaSonucu> hepsi = openRouteService.getAlternativeRoutes(kalkisLon, kalkisLat, varisLon, varisLat);
        if (hepsi.size() <= 1) {
            return List.of();
        }

        List<RotaSecenegi> sonuc = new ArrayList<>();
        int sira = 1;
        for (RotaSonucu alternatif : hepsi.subList(1, hepsi.size())) {
            sonuc.add(new RotaSecenegi(alternatif, "Alternatif " + sira, null));
            sira++;
        }
        return sonuc;
    }

    // Uzun mesafe: aday şehirler sırayla denenir, süre ve örtüşme testini geçenler kabul edilir
    private List<RotaSecenegi> araSehirlerIleBul(RotaSonucu ana, double kalkisLon, double kalkisLat,
                                                  double varisLon, double varisLat) {
        List<AdayKonum> adaylar = adaySehirleriBul(ana, kalkisLon, kalkisLat, varisLon, varisLat);
        if (adaylar.isEmpty()) {
            return List.of();
        }

        // Kota sınırı kadar aday aynı anda denenir (sırayla ~0,5 sn x 4); kabul yine aday sırasıyla yapılır
        long t0 = System.nanoTime();
        List<AdayKonum> denenecekler = adaylar.subList(0, Math.min(MAKS_ORS_CAGRISI, adaylar.size()));
        List<RotaSonucu> adayRotalari = new ArrayList<>();
        try (ExecutorService yurutucu = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<RotaSonucu>> istekler = denenecekler.stream()
                    .map(aday -> yurutucu.submit(() -> openRouteService.getRouteViaPoint(
                            kalkisLon, kalkisLat, aday.boylam(), aday.enlem(), varisLon, varisLat)))
                    .toList();
            for (int i = 0; i < istekler.size(); i++) {
                try {
                    adayRotalari.add(istekler.get(i).get());
                } catch (ExecutionException e) {
                    log.warn("Aday şehir '{}' için rota alınamadı: {}", denenecekler.get(i).ad(), e.getCause().getMessage());
                    adayRotalari.add(null);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return List.of();
                }
            }
        }

        long t1 = System.nanoTime();
        List<RotaSonucu> kabulEdilenler = new ArrayList<>();
        kabulEdilenler.add(ana);
        List<RotaSecenegi> sonuc = new ArrayList<>();
        for (int i = 0; i < denenecekler.size() && sonuc.size() < HEDEF_ALTERNATIF_SAYISI; i++) {
            RotaSonucu adayRota = adayRotalari.get(i);
            if (adayRota == null || adayRota.sureSn() > SURE_CARPANI * ana.sureSn()) {
                continue;
            }
            boolean fazlaOrtusuyor = kabulEdilenler.stream()
                    .anyMatch(digeri -> ortusmeOrani(adayRota.wkt(), digeri.wkt()) >= ORTUSME_ESIGI);
            if (fazlaOrtusuyor) {
                continue;
            }
            kabulEdilenler.add(adayRota);
            String ad = denenecekler.get(i).ad();
            sonuc.add(new RotaSecenegi(adayRota, ad + " üzerinden", ad));
        }
        log.info("Ara şehir: {} aday, ORS {} ms, eleme {} ms", denenecekler.size(),
                (t1 - t0) / 1_000_000, (System.nanoTime() - t1) / 1_000_000);
        return sonuc;
    }

    // Elips içinde (kalkış+varış mesafesinin 1.35 katına kadar dolambaç), ana hatta yeterince uzak,
    // uç noktalara yeterince uzak şehir/kasabalar; önem ve nüfusa göre sıralı en fazla 6 aday
    // Paket-özel: testte JdbcClient'ı mock'lamak yerine spy ile bu adım doğrudan stub'lanır
    List<AdayKonum> adaySehirleriBul(RotaSonucu ana, double kalkisLon, double kalkisLat,
                                      double varisLon, double varisLat) {
        return jdbcClient.sql("""
                WITH uc AS (
                    SELECT ST_Distance(
                        ST_SetSRID(ST_MakePoint(:kalkisLon, :kalkisLat), 4326)::geography,
                        ST_SetSRID(ST_MakePoint(:varisLon, :varisLat), 4326)::geography
                    ) AS mesafe
                )
                SELECT y.ad AS ad,
                       ST_Y(y.konum::geometry) AS enlem,
                       ST_X(y.konum::geometry) AS boylam
                FROM yerlesimler y, uc
                WHERE y.tur IN ('city', 'town')
                  AND ST_Distance(y.konum, ST_SetSRID(ST_MakePoint(:kalkisLon, :kalkisLat), 4326)::geography)
                    + ST_Distance(y.konum, ST_SetSRID(ST_MakePoint(:varisLon, :varisLat), 4326)::geography)
                      <= 1.35 * uc.mesafe
                  AND ST_Distance(y.konum, ST_SetSRID(ST_GeomFromEWKT(:anaWkt), 4326)::geography)
                      >= GREATEST(10000, 0.08 * :anaMesafe)
                  AND ST_Distance(y.konum, ST_SetSRID(ST_MakePoint(:kalkisLon, :kalkisLat), 4326)::geography) >= 20000
                  AND ST_Distance(y.konum, ST_SetSRID(ST_MakePoint(:varisLon, :varisLat), 4326)::geography) >= 20000
                ORDER BY y.onem DESC, y.nufus DESC NULLS LAST
                LIMIT :limit
                """)
                .param("kalkisLon", kalkisLon)
                .param("kalkisLat", kalkisLat)
                .param("varisLon", varisLon)
                .param("varisLat", varisLat)
                .param("anaWkt", ana.wkt())
                .param("anaMesafe", ana.mesafeM())
                .param("limit", MAKS_ADAY_SEHIR)
                .query(AdayKonum.class)
                .list();
    }

    // Adayın, daha önce kabul edilen rotanın 500 m tamponu içinde kalan uzunluğunun oranı (0-1)
    // Paket-özel: testte JdbcClient'ı mock'lamak yerine spy ile bu adım doğrudan stub'lanır
    double ortusmeOrani(String adayWkt, String digerWkt) {
        Double oran = jdbcClient.sql("""
                WITH aday AS (
                    SELECT ST_SimplifyPreserveTopology(ST_SetSRID(ST_GeomFromEWKT(:adayWkt), 4326), 0.0005) AS hat
                ),
                tampon AS (
                    SELECT ST_Buffer(ST_SimplifyPreserveTopology(ST_SetSRID(ST_GeomFromEWKT(:digerWkt), 4326), 0.0005)::geography, 500)::geometry AS alan
                )
                SELECT COALESCE(
                    ST_Length(ST_Intersection(a.hat, t.alan)::geography)
                        / NULLIF(ST_Length(a.hat::geography), 0),
                    0)
                FROM aday a, tampon t
                """)
                .param("adayWkt", adayWkt)
                .param("digerWkt", digerWkt)
                .query(Double.class)
                .single();
        return oran == null ? 0.0 : oran;
    }

    // sonuc: ORS/SQL'den gelen ham rota; ad: kullanıcıya gösterilecek isim; uzerinden: ara şehir adı (varsa)
    public record RotaSecenegi(RotaSonucu sonuc, String ad, String uzerinden) {
    }
}
