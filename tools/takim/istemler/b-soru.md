# Görev: Backend — yere soru sor (Gemini), Google yorumlarını istekten çıkar

Önce `AGENTS.md`, `docs/api-sozlesme.md` ("Yere soru sor — Aşama 9"), `com.seyyah.detay` paketi (özellikle `OzetServisi`:
Vikipedi metni çekme, Gemini çağrısı, günlük kota, `etkin()`), `com.seyyah.guvenlik.GuvenlikAyari` ve
`com.seyyah.hata.ApiIstisnasi` dosyalarını oku. **Commit atma.** URI'leri `UriComponentsBuilder...encode().build().toUri()`
ile kur. Yeni yapılandırma anahtarı gerekiyorsa `application.yaml`'a EKLE (önceki görevde unutuldu, canlıda özellik çalışmadı).

## 1. Google yorumları
`GooglePuanServisi` FieldMask'lerinden `places.reviews` ve `reviews`'ı çıkar (yorum dönmüyor ve pahalı SKU'ya sayılıyor olabilir).
`GoogleDetay.yorumlar` kalsın ama her zaman boş liste.

## 2. Ortak Gemini çağrısı
`OzetServisi`'ndeki Gemini HTTP çağrısını ve Vikipedi metni çekmeyi küçük bir ortak sınıfa taşı (ör. `GeminiIstemcisi`,
`VikipediMetni`) ve hem özet hem soru için kullan; davranış değişmesin (mevcut testler geçmeli).
Gemini isteğinde talimatı `systemInstruction` alanıyla ver: `{"systemInstruction":{"parts":[{"text": ...}]}, "contents": [...]}`.

## 3. Soru servisi (`com.seyyah.detay.SoruServisi`)
- Bağlam: yer adı, kategori, tür, çalışma saatleri, ücret, Vikipedi giriş metni (varsa, en fazla 3000 karakter).
- Sistem talimatı (Türkçe): "Sen Seyyah uygulamasında bir gezi rehberisin. Yalnızca verilen BİLGİLER'e dayanarak 3-4 cümleyle,
  Türkçe yanıt ver. Bilgilerde olmayan şeyi uydurma; bilgi yetersizse bunu açıkça söyle. Soru bu yerle ilgili değilse
  kibarca yalnızca bu yer hakkında yardım edebileceğini söyle. Kişisel veri isteme, tıbbi/hukuki tavsiye verme."
- Kullanıcı mesajı: `BİLGİLER:\n...\n\nSORU: <soru metni>`. Hazır soruların metni sözleşmedeki Türkçe karşılıklar.
- Yanıt: kırp, markdown işaretlerini temizle, 700 karakterle sınırla.
- Günlük kota: `OzetServisi` ile aynı `gemini` günlük sayacı (300). Dolduysa → 429 (`ApiIstisnasi`).
- Gemini hatası / anahtar yok → 503 "Yapay zekâ şu an yanıt veremiyor, biraz sonra tekrar deneyin."

## 4. Hazır soru — `POST /api/yerler/{id}/hazir-soru` (herkese açık)
- V9: `yer_soru_onbellek (yer_id BIGINT REFERENCES places(id) ON DELETE CASCADE, soru TEXT, cevap TEXT NOT NULL,
  olusturulma TIMESTAMPTZ NOT NULL DEFAULT now(), PRIMARY KEY (yer_id, soru))`.
- Önce önbellek; yoksa üret ve yaz (yalnızca başarılı cevap yazılır). `onbellekten` alanını doğru doldur.

## 5. Serbest soru — `POST /api/uye/yerler/{id}/soru` (giriş gerekli)
- `GuvenlikAyari.KORUMALI_YOLLAR`'a `/api/uye/**` ekle (hem `authenticated()` hem token çözümleme bu listeyi kullanıyor).
- `metin` `@Size(min = 3, max = 200)`, `@NotBlank`.
- Kişisel sınır: `api_kullanim`'da servis `soru-uye-<kullaniciId>`, `ay` sütununa saat anahtarı (`2026-09-25T14`), 10'u aşarsa 429.
  (Sayaç artırma mevcut atomik desenle.) Günlük `gemini` kotası da ayrıca sayılır.

## Testler
- `SoruServisiTest` (`MockRestServiceServer`): istekte `systemInstruction` ve BİLGİLER bağlamı var; markdown temizliği; 503 → istisna.
- Controller WebMvcTest'leri (`@Import(GuvenlikAyari.class)` + `@TestPropertySource(jwt.gizli=...)`):
  hazır soru geçersiz anahtar → 400; serbest soru tokensız → 401; `jwt()` ile 200; 201 karakter → 400.
- Testcontainers (`PostgisTestDestegi`; yerelde SKIP normal): hazır cevap bir kez üretilir, ikinci istekte `onbellekten=true`
  ve Gemini çağrılmaz; kişisel saatlik sınır 11. istekte 429.

## Bitirme
`JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2026.2.3/jbr" ./mvnw -q test` hatasız. Raporda, yerelde SKIP olan testleri
"geçti" diye yazma. Kısa rapor.
