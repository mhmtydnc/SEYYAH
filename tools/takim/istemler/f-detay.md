# Görev: Frontend — yer detayı paneli

Önce `AGENTS.md` ve `docs/api-sozlesme.md` "Yer detayı — Aşama 7" bölümünü oku. Sadece `frontend/` altında çalış.
**Commit atma.** Backend henüz hazır olmayabilir; sözleşmeye göre yaz. npm önbellek hatasında `npm ci --cache ./.npm-cache`.

## Davranış
- Yan paneldeki yer kartına (adına) ya da harita popup'ındaki "Detay" bağlantısına tıklayınca panel **detay görünümüne**
  geçer (harita görünür kalır, seçili yer haritada odaklanır). "← Listeye dön" ile aynı sekme ve kaydırma konumuna dönülür.
- `GET /api/yerler/{id}/detay` çağrılır; yüklenirken iskelet (skeleton) görünüm; hata olursa kısa mesaj + yine de listede
  zaten bilinen alanlar (ad, kategori, yola uzaklık, saatler, ücret, site) gösterilir.
- Aynı oturumda aynı yer için ikinci istek atılmaz (basit bellek içi önbellek: `Map<id, Promise>`).

## İçerik (sırasıyla, olmayan bölüm hiç çizilmez)
1. Görsel: `wikidata.gorsel.url`, genişliğe oturur, en-boy oranı 3:2, `loading="lazy"`, `alt` = yer adı. Altında küçük yazı:
   "Fotoğraf: <yazar> · <lisans>" ve `sayfa`'ya bağlantı (Wikimedia Commons). Yazar yoksa sadece lisans.
2. Ad, Türkçe kategori (`kategoriAdi`), yola uzaklık.
3. Google puanı: yıldız + `puan` (tek ondalık, tr-TR) + "(1.234 değerlendirme)" + yanında **"Google"** yazısı
   (atıf zorunlu) + `haritaBaglantisi` → "Google Haritalar'da gör".
4. `wikidata.aciklama` (ilk harf büyük).
5. Çalışma saatleri, ücret, web sitesi (mevcut `guvenliSite` kontrolüyle), `vikipedi` bağlantısı ("Vikipedi'de oku").
6. **"+ Durak ekle / Duraktan çıkar"** düğmesi (mevcut durak mantığı ile aynı).
- Tüm dış bağlantılar `target="_blank" rel="noopener noreferrer"`.

## Mobil
768 px altında detay, ekranın altından açılan bir kart (bottom sheet) olarak gösterilir: en fazla ekran yüksekliğinin %75'i,
kendi içinde kayar, üstte kapatma tutamacı/düğmesi. 390 px'te yatay taşma olmasın.

## Test
- Detay bileşeninin koşullu bölümleri için birim testi (vitest; `wikidata=null`, `google=null`, görselde yazar yok durumları).
  DOM testi için gerekiyorsa `@testing-library/react` + `jsdom` ekleyebilirsin (dev bağımlılığı).

## Bitirme
`cd frontend && npm test && npm run build` hatasız. Kısa rapor.
