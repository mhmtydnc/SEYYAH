# Görev: Yer arama ve tam rota endpoint'leri (backend)

Önce `AGENTS.md` ve `docs/api-sozlesme.md` dosyalarını oku. Sözleşmedeki **"Yer arama"** ve **"Rota"** bölümlerini uygula.

## Yapılacaklar
1. `com.seyyah.konum` paketi:
   - `KonumServisi`: ORS `GET /geocode/autocomplete?text=..&size=..&boundary.country=TR` çağırır. Mevcut
     `OpenRouteService`'teki RestClient kurulumunu ve hata çevirisini (`hatayiCevir`) örnek al; ORS anahtarı
     aynı (`ors.api.key`). GeoJSON `features[].properties.name` → ad, `.label` → etiket,
     `geometry.coordinates` → [boylam, enlem].
   - `KonumSonucu` record (ad, etiket, enlem, boylam), `KonumController` → `GET /api/konum/ara`.
2. `OpenRouteService`'e rota geometrisini, mesafeyi (`features[0].properties.summary.distance`) ve süreyi
   (`summary.duration`) döndüren bir metot ekle (ör. `RotaSonucu getRoute(...)` → koordinat listesi, WKT,
   mesafe, süre). Mevcut `getRouteWkt` ve `/api/routes/routeStreet` bozulmasın (ona dokunma, istersen
   yeni metodu kullanacak şekilde içini sadeleştir).
3. `GET /api/rota` (yeni controller ya da `RouteController` içinde): ORS'u **bir kez** çağır, sonra
   `placeRepository.koridorda(wkt, tur, yaricap, limit)`'i `gezi`, `mola`, `destek` için ayrı ayrı çağır.
   Yanıt sözleşmedeki JSON. Parametre doğrulaması mevcut stilde (`@DecimalMin`, `@Min` ...).
4. Testler: mevcut `RouteControllerTest` / `OpenRouteServiceTest` stilinde
   - `KonumController` için WebMvcTest (başarılı, q çok kısa → 400)
   - `/api/rota` için WebMvcTest (üç tür de sorgulanıyor, ORS tek sefer çağrılıyor, geçersiz koordinat → 400)
   - ORS geocode yanıtını ayrıştırma testi (`MockRestServiceServer` ile, `OpenRouteServiceTest`'e bak)

## Dokunma
`pom.xml`, güvenlik/kimlik doğrulama, `db/migration`, `frontend/`. Bunları başka ajanlar yapıyor.

## Bitirme
`JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2026.2.3/jbr" ./mvnw -q test` çalıştır.
`SeyyahApplicationTests.contextLoads` DB olmadığı için zaten düşüyor, diğer her şey geçmeli.
Türkçe commit mesajıyla commit'le. Kısa rapor yaz.
