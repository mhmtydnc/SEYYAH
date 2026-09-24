# Görev: Frontend — rotaya durak ekleme, zaman çizelgesi ve yolculuk modu

Önce `AGENTS.md` ve `docs/api-sozlesme.md` oku ("Rota" altındaki Aşama 6 `araNokta` notu, "Duraklı rota — Aşama 6",
"Kayıtlı rotalar" Aşama 6 alanları). Sadece `frontend/` altında çalış. **Commit atma.** Backend bu uçları henüz
sunmuyor olabilir; sözleşmeye göre yaz, backend'i çalıştırma. npm önbellek hatasında `npm ci --cache ./.npm-cache`.

## 1. Durak ekleme (ana sayfa)
- Yan paneldeki her yer kartında ve harita popup'ında **"+ Durak ekle" / "Duraktan çıkar"** düğmesi (en fazla 10 durak).
- Durak seçimi **seçili rotaya** aittir; kullanıcı başka rotaya geçince duraklar korunur ama sıralama yeni rotanın
  `yolOrani`'na göre yapılır (durak o rotanın listesinde yoksa en yakın konumuna göre değil, eklendiği sırayla sona konur).
- "Duraklarım" bölümü (yan panelde sekmelerin üstünde, durak varsa görünür): duraklar **`yolOrani` sırasına göre**,
  her birinde tahmini varış saati, kalış süresi (dakika, düzenlenebilir sayı alanı), çıkarma düğmesi.
- Duraklar değişince (400 ms debounce) `POST /api/rota/duraklu` çağrılır: noktalar = kalkış, [seçili rotanın `araNokta`'sı
  varsa ve duraklardan hiçbiri onun yerini tutmuyorsa kendi `yolOrani` konumuna göre araya], duraklar, varış.
  Basitlik için: `araNokta` varsa durakların yanına "sanal durak" olarak koy, sırala, `durakId` olmadan gönder.
- Haritada: duraklı rota çizgisi seçili rotanın yerine çizilir; duraklar numaralı işaretle (1, 2, 3…) gösterilir.
- Özet: toplam mesafe/süre, **sapma** (duraklı süre − seçili rota süresi, "+45 dk yol") ve toplam kalış süresi.

## 2. Zaman çizelgesi
- "Kalkış saati" alanı (varsayılan: şimdi, 5 dakikaya yuvarlanmış).
- Her durağın varış saati = kalkış + önceki bacakların süreleri + önceki durakların kalış süreleri. Varış noktası da listelenir.
- Varsayılan kalış süreleri `src/rota/kalisSureleri.ts` içinde kategoriye göre: museum 90, archaeological_site 90,
  castle 60, ruins 45, attraction 60, viewpoint 20, waterfall 40, beach 120, park 30, nature_reserve 90, garden 30,
  place_of_worship 20, monument 15, memorial 15, artwork 10, restaurant 60, cafe 30, fuel 10, toilets 5, parking 5,
  diğerleri 30.
- Durağın `calismaSaatleri` varsa varış saatinin yanında gösterilir (ayrıştırma yapma, sadece metin; ileride eklenecek).

## 3. Kaydetme ve açma
- Üyeyse "Rotayı kaydet" artık `uzerinden` (seçili rotanın araNokta'sı) ve `duraklar`'ı da gönderir.
- `/rotalarim` → "Aç": kalkış/varış/duraklar URL üzerinden değil `sessionStorage` ile ana sayfaya taşınır
  (URL'de sadece `?kayitli=<id>`); ana sayfa rotayı oluşturur, `uzerinden` adıyla eşleşen alternatifi seçer, durakları geri yükler.

## 4. Yolculuk modu (`/yolculuk`)
- Ana sayfada durak varken **"Yolculuğa başla"** düğmesi → plan (noktalar, adlar, bacaklar, kalış süreleri, kalkış saati)
  `localStorage`'a yazılır (`seyyah-yolculuk`) ve `/yolculuk`'a gidilir. Sayfa yenilense de kaldığı yerden devam eder.
- Mobil öncelikli tam ekran düzen: büyük "Sıradaki durak" kartı (ad, kategori, tahmini varış, kalış süresi),
  kalan durak listesi, ilerleme (2/5).
- **"Git"** düğmesi: sıradaki noktaya yol tarifi. iOS'ta (`/iPad|iPhone|iPod/` user agent) `https://maps.apple.com/?daddr=<enlem>,<boylam>&dirflg=d`,
  diğerlerinde `https://www.google.com/maps/dir/?api=1&destination=<enlem>,<boylam>&travelmode=driving`. Yeni sekmede aç.
- **"Tüm rotayı haritada aç"**: Google Maps `origin=My+Location`, `destination`=varış, `waypoints`=kalan duraklar (`|` ile, en fazla 9).
- **"Vardım"** → sıradakine geç; **"Bu durağı atla"**; **"Yolculuğu bitir"** (localStorage temizlenir, ana sayfaya döner).
- Konum izni verilirse (`navigator.geolocation.watchPosition`, yüksek doğruluk kapalı): sıradaki durağa kuş uçuşu mesafe
  gösterilir; 300 m içine girilince kart "Yaklaştın" vurgusu alır. İzin yoksa sessizce gizlenir.
- Tahmini varış saatleri, "Vardım" basıldığı andaki gerçek saate göre yeniden hesaplanır.

## Yapı ve testler
- Saf fonksiyonlar `src/rota/` altında: durak sıralama (yolOrani + araNokta), zaman çizelgesi hesabı, harita bağlantısı
  üretimi (iOS/diğer, waypoint sınırı). Bunlar için vitest birim testleri yaz.
- Mobil: 390 px genişlikte yatay taşma olmasın (`minmax(0, 1fr)` kullan).

## Bitirme
`cd frontend && npm test && npm run build` hatasız. Kısa rapor: dosyalar, test sayısı, belirsiz kararlar.
