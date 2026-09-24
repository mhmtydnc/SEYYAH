# Görev: Backend — yapay zekâ özeti (Gemini) ve Google yorumları

Önce `AGENTS.md`, `docs/api-sozlesme.md` ("Yer detayı — Aşama 7" ve "Aşama 8 eklemeleri") ve `com.seyyah.detay` paketinin
tamamını oku (`WikidataServisi`, `GooglePuanServisi`, `YerDetayServisi` — önbellek ve kota mantığını aynen izle).
**Commit atma.** URI'leri `UriComponentsBuilder...encode().build().toUri()` ile kur; `uri(String)`'e elle kodlanmış metin
VERME (çift kodlama hatası yaşandı, bkz. `WikidataServisi.gorselGetir`).

## 1. Şema — `V8__ozet.sql`
- `yer_detay_onbellek`'e `ozet JSONB`, `ozet_zamani TIMESTAMPTZ`.
- `api_kullanim.ay` → `TEXT` (günlük anahtar tutabilsin; mevcut `YYYY-MM` değerleri geçerli kalır).

## 2. Özet (`com.seyyah.detay.OzetServisi`)
- Ayarlar: `gemini.anahtar: ${GEMINI_API_KEY:}` (boşsa hiç çağrı yok, `null`), `gemini.model: gemini-flash-lite-latest`.
- Girdi: `WikidataDetay.vikipedi` adresinden dil ve başlık çıkar →
  `https://<dil>.wikipedia.org/api/rest_v1/page/summary/<başlık>` (User-Agent mevcut `Seyyah/1.0 (...)`) → `extract`.
  `extract` 80 karakterden kısaysa özet yok (`null`).
- Gemini: `POST https://generativelanguage.googleapis.com/v1beta/models/<model>:generateContent`, başlık `x-goog-api-key`,
  gövde `{"contents":[{"parts":[{"text": İSTEM}]}],"generationConfig":{"maxOutputTokens":200,"temperature":0.3}}`.
  İSTEM (lider test etti, iyi sonuç veriyor):
  `Aşağıdaki Vikipedi metnine dayanarak bir gezgin için 2-3 cümlelik, Türkçe, abartısız bir tanıtım yaz: yerin ne olduğu ve neden görülmeye değer olduğu. Metinde olmayan bilgi ekleme.\n\nMETİN:\n` + extract (en fazla 2000 karakter).
  Yanıt `candidates[0].content.parts[0].text`: kırp, markdown işaretlerini (`*`, `#`, `_`) temizle, 600 karakterle sınırla.
- **Günlük kota:** `api_kullanim`'da servis `gemini`, `ay` sütununa gün (`2026-09-25`); mevcut atomik artırma deseniyle,
  300'ü geçerse çağrı yok.
- **Hatalar:** Gemini sık sık 503 (aşırı yük) ve 429 dönüyor; lider testinde de 503 aldı. Her hata → `null` ve önbelleğe
  YAZILMAZ (sonraki istekte tekrar denensin). Tekrar deneme döngüsü kurma.

## 3. Önbellek (`YerDetayServisi`)
- Özet üretildiyse süresiz saklanır (`ozet`, `ozet_zamani`); `ozet` varsa bir daha üretilmez.
- Vikipedi yoksa ya da metin kısaysa "özet yok" durumu da yazılabilir (ör. `ozet_zamani` dolu, `ozet` null) ki her
  seferinde Vikipedi'ye gidilmesin — Google'daki `eslesmeYok` deseni gibi. Gemini/ağ hatası ise yazılmaz.

## 4. Google yorumları (`GooglePuanServisi`)
- FieldMask'lere yorumları ekle: searchText'te `places.reviews`, place details'te `reviews`.
- İlk 3 yorum → `yazar` (`authorAttribution.displayName`), `yazarBaglantisi` (`authorAttribution.uri`), `puan` (`rating`),
  `metin` (`text.text`, yoksa `originalText.text`; 600 karakterle sınırla), `zaman` (`relativePublishTimeDescription`).
- `GoogleDetay`'a `yorumlar` listesi (yoksa boş liste). 30 günlük önbellek kuralı aynen geçerli.

## Testler
- `OzetServisiTest` (`MockRestServiceServer`): başarılı özet (markdown temizliği), kısa extract → null, 503 → null,
  anahtar yok → hiç çağrı yok, Türkçe karakterli başlıkta tek kodlanmış URL (`An%C4%B1tkabir`).
- `GooglePuanServisiTest`: yorum ayrıştırma (en fazla 3, uzun metin kesilir).
- `YerDetayOnbellekTest` (Testcontainers, yerelde SKIP normal): özet bir kez üretilir, ikinci istekte `OzetServisi` çağrılmaz;
  hata sonucu önbelleğe yazılmaz.

## Bitirme
`JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2026.2.3/jbr" ./mvnw -q test` hatasız. Kısa rapor.
