# SEYYAH — Ajan Takımı Ortak Kuralları

Bu dosyayı Claude Code, Codex ve Antigravity (Gemini) ajanlarının hepsi okur. Kısa tut.

## Proje
Seyyahlar için web uygulaması: başlangıç–bitiş arasında rota çizilir, rota koridorundaki
gezilecek yerler (`gezi`), mola yerleri (`mola`) ve destek noktaları (`destek`) listelenir.

- Backend: Spring Boot 3.5 / Java 21, paket `com.seyyah`, port 9090
- Veritabanı: PostgreSQL + PostGIS (Azure), Flyway (`src/main/resources/db/migration`), JPA `ddl-auto: validate`
- Rota: OpenRouteService (`com.seyyah.route.OpenRouteService`)
- Veri hazırlama: `tools/osm/donustur.py` (OSM → places tablosu)
- Frontend: `frontend/` klasörü (henüz yok, takımca kurulacak)

## Mevcut API
- `GET  /api/routes/routeStreet?startLon&startLat&endLon&endLat&tur&yaricap&limit` → rota + koridordaki yerler
- `GET|POST /api/places/koridor` (WKT ile) → koridordaki yerler
- Yanıt öğesi: `KoridorYeri` (id, ad, kategori, tur, enlem, boylam, yolaUzaklikM, yolOrani, ucret, calismaSaatleri, wikidataId, website)

## Kurallar
1. Kod, yorum, commit mesajı ve değişken adları **Türkçe** (mevcut kodla aynı üslup). Yorumları az ve "neden"i anlatan tut.
2. Sadece sana verilen görevin kapsamındaki dosyalara dokun. Kapsam dışı bir sorun görürsen raporuna yaz, düzeltme.
3. Sırları (şifre, API anahtarı) asla koda/commit'e yazma. `application-local.yaml` ve `.env` git dışıdır.
4. Mevcut migration dosyalarını değiştirme; şema değişikliği için yeni `V<n>__aciklama.sql` ekle.
5. Native SQL yorumlarında tek tırnak (') kullanma — Spring Data metin başlangıcı sanıyor.
6. Codex ve Antigravity ajanları **commit atmaz** (sandbox git dizinine yazamıyor); değişiklikleri çalışma
   ağacında bırakır, lider inceleyip commit'ler. Claude alt ajanları kendi dalına commit'ler.
   Hiçbir ajan push/merge yapmaz; birleştirmeyi takım lideri yapar.
7. Raporun kısa olsun: ne yaptın, hangi dosyalar, nasıl test ettin, açık kalanlar.

## Derleme / test
- Backend: `./mvnw -q test` (JDK 21+ gerekir). DB gerektiren testler yerelde çalışmayabilir; bunu raporda belirt.
- Frontend: `frontend/` altında `npm run build` ve varsa `npm test`.
