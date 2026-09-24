# Görev: Backend — yer detayı (Wikidata + Google puanı, önbellekli)

Önce `AGENTS.md` ve `docs/api-sozlesme.md` "Yer detayı — Aşama 7" bölümünü oku; yanıt biçimine BİREBİR uy.
**Commit atma.** Örnek üslup: `com.seyyah.konum.KonumServisi` (RestClient, hata yönetimi), `com.seyyah.route.OpenRouteService`.

## 1. Şema — `V7__yer_detay.sql`
- `yer_detay_onbellek`: `yer_id BIGINT PRIMARY KEY REFERENCES places(id) ON DELETE CASCADE`, `wikidata JSONB`,
  `wikidata_zamani TIMESTAMPTZ`, `google_place_id TEXT`, `google JSONB`, `google_zamani TIMESTAMPTZ`.
- `api_kullanim`: `ay CHAR(7)` (ör. 2026-09), `servis TEXT`, `sayi INT NOT NULL DEFAULT 0`, `PRIMARY KEY (ay, servis)`.
- Entity gerekmez; `JdbcClient` kullan (`ddl-auto: validate` etkilenmesin).

## 2. Wikidata (`com.seyyah.detay.WikidataServisi`)
- Tüm isteklerde `User-Agent: Seyyah/1.0 (https://github.com/mhmtydnc/SEYYAH)` (Wikimedia kuralı), zaman aşımı mevcut ayarlarla.
- `https://www.wikidata.org/w/api.php?action=wbgetentities&ids=<Q>&props=descriptions|claims|sitelinks&languages=tr|en&format=json`
  → açıklama (tr, yoksa en), Vikipedi (`sitelinks.trwiki`, yoksa `enwiki`; `https://<dil>.wikipedia.org/wiki/<başlık, boşluk→_, URL kodlu>`),
  P18 dosya adı (ilk değer).
- P18 varsa `https://commons.wikimedia.org/w/api.php?action=query&titles=File:<ad>&prop=imageinfo&iiprop=url|extmetadata&iiurlwidth=640&format=json`
  → `thumburl`, `descriptionurl`, `extmetadata.Artist.value` (HTML etiketlerini sil, boşlukları sadeleştir, 200 karakterle kes),
  `extmetadata.LicenseShortName.value`.
- Her hata/eksik alan: ilgili alan `null`, istisna fırlatma, uyarı logla.

## 3. Google puanı (`com.seyyah.detay.GooglePuanServisi`)
- Anahtar `google.places.anahtar: ${GOOGLE_PLACES_API_KEY:}` (boşsa servis hiç çağrı yapmaz, `null` döner). `application.yaml`'a ekle.
- **Kota:** çağrıdan ÖNCE `api_kullanim` satırını atomik artır
  (`INSERT ... ON CONFLICT (ay, servis) DO UPDATE SET sayi = api_kullanim.sayi + 1 RETURNING sayi`); dönen değer 900'ü
  aşıyorsa çağrı yapma, `null` dön. Servis adı `google_places`.
- `place_id` önbellekte yoksa: `POST https://places.googleapis.com/v1/places:searchText`, başlıklar `X-Goog-Api-Key`,
  `X-Goog-FieldMask: places.id,places.rating,places.userRatingCount,places.googleMapsUri,places.location`,
  gövde `{"textQuery": "<ad>", "languageCode": "tr", "maxResultCount": 1, "locationBias": {"circle": {"center": {"latitude":..,"longitude":..}, "radius": 500}}}`.
  Dönen yerin konumu bizim konuma **1 km'den uzaksa** eşleşme yok say (`google_place_id` yazma, `null` dön).
- `place_id` biliniyorsa: `GET https://places.googleapis.com/v1/places/<id>`, FieldMask `rating,userRatingCount,googleMapsUri`.
- `rating` yoksa (puanı olmayan yer) `google` = `null` ama `place_id` saklanır.

## 4. Birleştirme (`com.seyyah.detay.YerDetayServisi` + `YerDetayController`)
- `GET /api/yerler/{id}/detay`: yer `places`'tan (yoksa 404, mevcut `ApiIstisnasi` ile).
- Önbellek: `wikidata_zamani` 30 günden eskiyse ya da yoksa Wikidata yeniden çekilir; `google_zamani` 30 günden eskiyse
  Google yeniden çekilir (`place_id` korunur). Sonuçlar `INSERT ... ON CONFLICT (yer_id) DO UPDATE` ile yazılır.
  Başarısız dış çağrı önbelleğe **yazılmaz** (bir sonraki istekte tekrar denensin).
- Güvenlik: `/api/yerler/**` herkese açık (GuvenlikAyari'nda anyRequest().permitAll zaten; kontrol et).

## Testler
- `WikidataServisiTest` (`MockRestServiceServer`): tr açıklama/trwiki, tr yoksa en'e düşme, P18 + Commons ayrıştırma (Artist HTML temizliği), P18 yok.
- `GooglePuanServisiTest`: anahtar boşsa hiç çağrı yok; searchText eşleşme; 1 km'den uzak sonuç reddedilir; place_id varsa Details çağrısı.
  Kota için `JdbcClient` mock'u (ya da kota kontrolünü ayrı küçük sınıfa al ve onu mock'la).
- `YerDetayController` WebMvcTest (`@Import(GuvenlikAyari.class)` + `@TestPropertySource(jwt.gizli=...)`): 200 ve olmayan id → 404.
- Testcontainers (`PostgisTestDestegi`'den, yerelde SKIP normal): kota artırma SQL'i ve önbellek upsert'i.

## Bitirme
`JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2026.2.3/jbr" ./mvnw -q test` hatasız. Kısa rapor.
