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

## Rota (herkese açık)
`GET /api/rota?kalkisEnlem&kalkisBoylam&varisEnlem&varisBoylam&yaricap=5000&limit=20`
- `limit`: her tür için en fazla yer sayısı (1–100), `yaricap`: 100–20000 m
```json
{
  "mesafeM": 452310,
  "sureSn": 17820,
  "geometri": [[29.01, 41.00], [29.05, 40.98]],
  "yerler": {
    "gezi":   [ /* KoridorYeri */ ],
    "mola":   [ /* KoridorYeri */ ],
    "destek": [ /* KoridorYeri */ ]
  }
}
```
`geometri`: GeoJSON sırası `[boylam, enlem]`. Leaflet için `[enlem, boylam]`'a çevrilmeli.

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

## Kayıtlı rotalar (giriş gerekli)
`POST /api/rotalarim`
```json
{ "baslik": "İstanbul - Kapadokya",
  "kalkis": { "ad": "İstanbul", "enlem": 41.01, "boylam": 28.97 },
  "varis":  { "ad": "Göreme",   "enlem": 38.64, "boylam": 34.83 } }
```
- 201 → `{ "id": 7, "baslik": "...", "kalkis": {...}, "varis": {...}, "olusturulma": "2026-09-24T14:30:00Z" }`

`GET /api/rotalarim` → aynı öğelerin listesi, yeniden eskiye
`DELETE /api/rotalarim/{id}` → 204; başkasının rotasıysa ya da yoksa 404
