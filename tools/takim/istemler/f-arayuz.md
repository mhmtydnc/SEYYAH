# Görev: Frontend (React) — rota ekranı, üyelik, kayıtlı rotalar

Önce `AGENTS.md` ve `docs/api-sozlesme.md` dosyalarını oku. Backend henüz bu endpoint'lerin bir kısmını
yazmıyor olabilir; sözleşmeye göre yaz, backend'i çalıştırmaya çalışma.

## Teknoloji
`frontend/` klasöründe Vite + React + TypeScript. Harita: `leaflet` + `react-leaflet` (OSM döşemeleri,
atıf metni görünür olsun). Yönlendirme: `react-router-dom`. Başka UI kütüphanesi ekleme; düz CSS
(CSS değişkenleri) yeterli. `vite.config.ts` içinde `/api` → `http://localhost:9090` proxy.

## Ekranlar (arayüz metinleri Türkçe)
1. **Ana sayfa `/`** — üyelik olmadan tam çalışır:
   - "Kalkış" ve "Varış" metin kutuları; yazdıkça (300 ms debounce, en az 2 karakter)
     `GET /api/konum/ara` ile öneri listesi, klavye (↑ ↓ Enter Esc) ile seçilebilir.
   - Yarıçap seçimi (1 / 5 / 10 / 20 km) ve "Rota oluştur" düğmesi (iki konum seçilmeden pasif).
   - `GET /api/rota` sonucu: haritada rota çizgisi, sığacak şekilde yakınlaştırma, kalkış/varış
     işaretleri, yerler türüne göre farklı renkte daire işaretler (gezi / mola / destek).
   - Yan panelde "Gezi / Mola / Destek" sekmeleri; her öğe: ad, kategori, yola uzaklık (km),
     varsa çalışma saatleri, ücret, web sitesi bağlantısı. Öğeye tıklayınca haritada o yere odaklan ve popup aç.
   - Üst bilgi: toplam mesafe (km) ve süre (sa dk).
   - Yükleniyor durumu ve ProblemDetail `detail` alanını gösteren hata mesajı.
   - Giriş yapılmışsa "Rotayı kaydet" düğmesi (`POST /api/rotalarim`, başlık varsayılanı "Kalkış - Varış").
     Giriş yoksa düğme yerine "Rotayı kaydetmek için giriş yap" bağlantısı.
2. **`/giris`** ve **`/kayit`** formları: istemci tarafı doğrulama (e-posta biçimi, şifre ≥ 8), sunucu
   hatasını göster, başarıda token'ı `localStorage`'a yaz ve ana sayfaya dön.
3. **`/rotalarim`** (giriş yoksa `/giris`'e yönlendir): liste, "Aç" (ana sayfaya kalkış/varış dolu
   gelir ve rota otomatik oluşturulur — URL sorgu parametreleriyle), "Sil" (onay ile).
4. Üst çubuk: logo "Seyyah", giriş yoksa "Giriş / Kayıt ol", varsa kullanıcı adı, "Rotalarım", "Çıkış".
   Açılışta token varsa `GET /api/auth/ben` ile doğrula; 401 gelirse token'ı sil.

## Yapı
- `src/api/` tek bir `istemci.ts` (fetch sarmalayıcı: Bearer başlığı, ProblemDetail → Error) ve tipler.
- `src/oturum/` React context (kullanıcı, girisYap, kayitOl, cikisYap).
- Mobil uyumlu: 768 px altında harita üstte, liste altta.
- Kod yorumları ve değişken adları Türkçe, az yorum.

## Bitirme
`cd frontend && npm install && npm run build` hatasız geçmeli (`tsc` dahil). `vitest` ile en az
`istemci.ts` hata dönüşümü ve koordinat çevirimi için birkaç birim testi ekle, `npm test` geçmeli.
`node_modules` ve `dist` commit'lenmesin (`frontend/.gitignore`). Türkçe commit mesajıyla commit'le.
Kısa rapor yaz.
