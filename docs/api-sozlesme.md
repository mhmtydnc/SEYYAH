# API Sözleşmesi (Aşama 3)

Backend ve frontend bu dosyaya göre paralel geliştirilir. Değişiklik gerekirse önce bu dosya güncellenir.

- Taban yol: `/api`. Geliştirmede frontend (Vite, :5173) `/api` isteklerini `http://localhost:9090`'a proxy'ler, CORS gerekmez.
- Hatalar RFC 9457 ProblemDetail: `{ "title": "...", "status": 400, "detail": "Kullanıcıya gösterilebilir Türkçe mesaj" }`
- Kimlik doğrulama: `Authorization: Bearer <token>` (JWT, HS256, 7 gün geçerli). Üyelik **isteğe bağlıdır**:
  rota ve yer arama herkese açıktır; sadece `/api/rotalarim/**` ve `/api/auth/ben` giriş ister.

## Yer arama (herkese açık)
`GET /api/konum/ara?q=Kapadokya&limit=5`  (q en az 2 karakter, limit 1–10, varsayılan 5)
```json
[ { "ad": "Göreme", "etiket": "Göreme, Nevşehir, Türkiye", "enlem": 38.64, "boylam": 34.83 } ]
```
ORS `/geocode/autocomplete` üzerinden, Türkiye ile sınırlı (`boundary.country=TR`). Sonuç yoksa `[]`.

## Rota (herkese açık) — Aşama 5: alternatifli
`GET /api/rota?kalkisEnlem&kalkisBoylam&varisEnlem&varisBoylam&yaricap=5000&limit=20`
- `limit`: her tür için en fazla yer sayısı (1–100), `yaricap`: 100–20000 m
```json
{
  "rotalar": [
    { "sira": 0, "ad": "En hızlı", "uzerinden": null,
      "mesafeM": 296400, "sureSn": 11400,
      "geometri": [[32.85, 39.92], [32.90, 39.88]],
      "yerler": { "gezi": [ /* KoridorYeri */ ], "mola": [], "destek": [] } },
    { "sira": 1, "ad": "Aksaray üzerinden", "uzerinden": "Aksaray",
      "mesafeM": 331200, "sureSn": 12900, "geometri": [], "yerler": { "gezi": [], "mola": [], "destek": [] } }
  ]
}
```
- `rotalar` en az 1, en fazla 3 öğe; `sira = 0` her zaman en hızlı (ana) rotadır, diğerleri süreye göre artan.
- `ad`: ana rota `"En hızlı"`; ORS'un kendi alternatifleri `"Alternatif 1"`, `"Alternatif 2"`; ara şehirden
  geçenler `"<Şehir> üzerinden"` (`uzerinden` alanında şehir adı).
- Alternatif bulunamazsa ya da alternatif hesaplama hata verirse yalnızca ana rota döner (istek hata vermez).
- Aşama 6: her rotada `"araNokta": { "ad": "Aksaray", "enlem": 38.37, "boylam": 34.03 }` ("X üzerinden" rotalarda),
  diğerlerinde `null`. Duraklı rota isteğinde seçili alternatifin korunması için gerekir.
- `geometri`: GeoJSON sırası `[boylam, enlem]`. Leaflet için `[enlem, boylam]`'a çevrilmeli.

`KoridorYeri`:
```json
{ "id": 1, "ad": "Anıtkabir", "kategori": "museum", "tur": "gezi", "enlem": 39.92, "boylam": 32.83,
  "yolaUzaklikM": 850, "yolOrani": 0.97, "ucret": null, "calismaSaatleri": "Mo-Su 09:00-17:00",
  "wikidataId": "Q193230", "website": null }
```
Hatalar: 400 geçersiz parametre, 404 rota bulunamadı, 503 kota doldu, 502/504 rota servisi sorunu.

## Üyelik
`POST /api/auth/kayit` → `{ "ad": "Mehmet", "eposta": "m@ornek.com", "sifre": "en az 8 karakter" }`
- 201 → `{ "token": "eyJ...", "kullanici": { "id": 1, "ad": "Mehmet", "eposta": "m@ornek.com" } }`
- 400 doğrulama hatası, 409 e-posta zaten kayıtlı

`POST /api/auth/giris` → `{ "eposta": "...", "sifre": "..." }`
- 200 → kayıtla aynı gövde; 401 `"E-posta veya şifre hatalı"`

`GET /api/auth/ben` (giriş gerekli) → `{ "id": 1, "ad": "Mehmet", "eposta": "m@ornek.com" }`; token yok/geçersiz → 401

## Duraklı rota (herkese açık) — Aşama 6
`POST /api/rota/duraklu` — seçilen durakları sırayla uğrayarak rota; bacak mesafe/süreleri.
```json
{ "noktalar": [
    { "enlem": 39.92, "boylam": 32.85 },
    { "enlem": 39.14, "boylam": 34.16, "durakId": 118 },
    { "enlem": 38.37, "boylam": 34.03 },
    { "enlem": 38.64, "boylam": 34.83 } ] }
```
- `noktalar`: kalkış, ara noktalar, varış — **gönderilen sırayla** (sıralamayı istemci yapar: durakları `yolOrani`'na göre
  dizer; seçili rota "X üzerinden" ise o şehir de ara nokta olarak eklenir, `durakId` olmadan). En az 2, en fazla 13 nokta (10 durak + ara şehir + kalkış + varış).
- 200 →
```json
{ "mesafeM": 312400, "sureSn": 12100,
  "geometri": [[32.85, 39.92], [32.90, 39.88]],
  "bacaklar": [ { "mesafeM": 151000, "sureSn": 5600 }, { "mesafeM": 98000, "sureSn": 3900 }, { "mesafeM": 63400, "sureSn": 2600 } ] }
```
- `bacaklar[i]`: `noktalar[i]` → `noktalar[i+1]` (uzunluk = nokta sayısı − 1).
- Hatalar Rota ile aynı; 400: nokta sayısı sınır dışı ya da koordinat geçersiz.

## Kayıtlı rotalar (giriş gerekli)
`POST /api/rotalarim`
```json
{ "baslik": "İstanbul - Kapadokya",
  "kalkis": { "ad": "İstanbul", "enlem": 41.01, "boylam": 28.97 },
  "varis":  { "ad": "Göreme",   "enlem": 38.64, "boylam": 34.83 } }
```
- 201 → `{ "id": 7, "baslik": "...", "kalkis": {...}, "varis": {...}, "olusturulma": "2026-09-24T14:30:00Z" }`
- Aşama 6 ile isteğe bağlı iki alan (istek ve yanıtta; eski kayıtlarda `null` / `[]`):
  `"uzerinden": { "ad": "Aksaray", "enlem": 38.37, "boylam": 34.03 }` ve
  `"duraklar": [ { "id": 118, "ad": "Kırşehir Kalesi", "kategori": "castle", "enlem": 39.14, "boylam": 34.16 } ]` (en fazla 10).

`GET /api/rotalarim` → aynı öğelerin listesi, yeniden eskiye
`DELETE /api/rotalarim/{id}` → 204; başkasının rotasıysa ya da yoksa 404

## Yer detayı (herkese açık) — Aşama 7
`GET /api/yerler/{id}/detay` → yer yoksa 404.
```json
{ "id": 118, "ad": "Kırşehir Kalesi", "kategori": "castle", "tur": "gezi", "enlem": 39.14, "boylam": 34.16,
  "ucret": null, "calismaSaatleri": "Tu-Su 09:00-17:00", "website": "https://...",
  "wikidata": {
    "aciklama": "Kırşehir'de bir kale",
    "vikipedi": "https://tr.wikipedia.org/wiki/K%C4%B1r%C5%9Fehir_Kalesi",
    "gorsel": { "url": "https://upload.wikimedia.org/...640px-...jpg", "sayfa": "https://commons.wikimedia.org/wiki/File:...",
                "yazar": "Ahmet Y.", "lisans": "CC BY-SA 4.0" } },
  "google": { "puan": 4.5, "yorumSayisi": 1234, "haritaBaglantisi": "https://maps.google.com/?cid=..." } }
```
- `wikidata`: yerin `wikidataId`'si yoksa ya da çekilemezse `null`. İçindeki `aciklama`, `vikipedi`, `gorsel` ayrı ayrı `null` olabilir.
  Açıklama ve Vikipedi Türkçe öncelikli, yoksa İngilizce. Görsel P18'den, 640 px küçük boyut; yazar HTML'den arındırılmış düz metin.
- `google`: anahtar tanımlı değilse, aylık kota dolduysa, eşleşme bulunamadıysa ya da hata olursa `null`.
- Önbellek sunucu tarafında: Wikidata 30 gün, Google puanı en fazla 30 gün (Google kuralı), `place_id` süresiz.
- Arayüz Google puanını gösterirken yanında "Google" atfı, görselin altında yazar ve lisans gösterir (zorunlu).
