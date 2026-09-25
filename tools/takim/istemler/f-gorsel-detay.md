# Görev: Frontend — haritada fotoğraflı açılır kutu, "Detay" sekmesi, beklemeden gösterilen bilgiler

Önce `AGENTS.md` ve `docs/api-sozlesme.md` "Aşama 10" bölümünü oku. Sadece `frontend/` altında çalış. **Commit atma.**
npm önbellek hatasında `npm ci --cache ./.npm-cache`. İlgili dosyalar: `src/harita/RotaHaritasi.tsx`,
`src/sayfalar/AnaSayfa.tsx`, `src/bilesenler/YerDetayi.tsx`, `src/api/tipler.ts`, `src/stil.css`.

## Sorun (kullanıcı şikâyeti)
Yere tıklayınca boş ekran görünüyor: detay paneli ilk açılışta dış kaynaklardan (3-5 sn) veri gelene kadar boş/iskelet
kalıyor; harita açılır kutusunda sadece ad ve küçük bir "Detay" bağlantısı var, fotoğraf yok.

## Yapılacaklar
1. **Tip:** `KoridorYeri`'ne `gorselUrl: string | null`.
2. **Harita açılır kutusu (`YerIsareti` Popup):** üstte `gorselUrl` varsa görsel (genişlik 220 px, yükseklik 130 px,
   `object-fit: cover`, köşeler yuvarlak, `loading="lazy"`, `alt` = ad); altında ad (kalın) ve Türkçe kategori; altta iki
   düğme yan yana: birincil stilde **"Detay"** ve "+ Durak ekle / Duraktan çıkar". Görsel yoksa görsel alanı hiç çizilmez.
   Görsel URL'si `guvenliSite` ile denetlenir.
3. **Yan panelde "Detay" sekmesi:** sekmeler Gezi | Mola | Destek | **Detay**. Detay sekmesi yalnızca bir yer seçiliyken
   görünür; "Detay" düğmesine (haritadan ya da listedeki karttan) basınca seçilir ve açılır. Sekmede `YerDetayi` gösterilir;
   üstteki "← Listeye dön" kaldırılır (sekmeler zaten var). Rota değişince detay sekmesi kapanır.
   Mobildeki alttan açılan kart (bottom sheet) davranışı kaldırılır; mobilde de panel içindeki sekme kullanılır.
4. **Beklemeden göster (`YerDetayi`):** listeden gelen alanlar (ad, Türkçe kategori, yola uzaklık, çalışma saatleri, ücret,
   web sitesi, "+ Durak ekle") **ilk karede** gösterilir. Yalnızca görsel, özet, Google puanı ve "Bu yer hakkında sor"
   bölümleri istek sürerken kendi yerlerinde küçük iskeletle bekler. Görsel için önce `gorselUrl` (listeden) hemen
   gösterilir; detaydaki görsel (yazar/lisansıyla) gelince onunla değiştirilir.
5. **Liste kartları:** `gorselUrl` varsa kartın solunda 56×56 küçük görsel (`object-fit: cover`, `loading="lazy"`);
   kartın adına basınca Detay sekmesi açılır.

## Test (vitest + testing-library)
- `YerDetayi`: istek bitmeden ad, kategori ve çalışma saatleri görünür; `gorselUrl` verilince görsel hemen görünür.
- Detay sekmesi: yer seçilmeden sekme yok; seçilince görünür ve etkin.

## Bitirme
`cd frontend && npm test && npm run build` hatasız; ana `index-*.js` 500 KB'ı geçmesin; 390 px'te yatay taşma olmasın.
Kısa rapor.
