# Görev: Frontend — alternatif rota seçimi

Önce `AGENTS.md` ve `docs/api-sozlesme.md` ("Rota — Aşama 5: alternatifli") oku. Sadece `frontend/` altında çalış.
**Commit atma.** Backend bu yanıtı henüz üretmiyor olabilir; sözleşmeye göre yaz, backend'i çalıştırma.

`GET /api/rota` yanıtı artık `{ rotalar: [...] }`. Eski tek-rota biçimi kalktı.

## Yapılacaklar
1. `src/api/tipler.ts`: `RotaYaniti` → `{ rotalar: Rota[] }`, `Rota` = `{ sira, ad, uzerinden, mesafeM, sureSn, geometri, yerler }`.
2. **Rota kartları** (özet satırının yerine, haritanın üstünde, yatay; mobilde kaydırılabilir):
   her kart: `ad`, mesafe (km), süre (sa dk); ana rota dışındakilerde ana rotaya göre fark (`+25 dk`, `+34 km`).
   Seçili kart vurgulu. Varsayılan seçim `sira = 0`. Tek rota varsa kartlar yerine bugünkü özet görünümü kalsın.
3. **Harita:** tüm rotalar çizilir; seçili rota koyu ve üstte (kalın), diğerleri gri/ince ve **tıklanınca seçilir**.
   `fitBounds` tüm rotaları kapsasın.
4. **Yan panel ve yer işaretleri** yalnızca seçili rotanın `yerler`'ini gösterir; rota değişince seçili yer sıfırlanır,
   sekme sayıları güncellenir.
5. **Rotayı kaydet** mevcut davranışla kalsın (kalkış/varış kaydedilir); başlıkta seçili rota ana rota değilse
   `"Kalkış - Varış (Aksaray üzerinden)"` gibi `uzerinden` eklensin.
6. Testler: fark metni biçimlendirme (`+1 sa 5 dk`, `+34 km`, negatif/sıfır farkta gösterilmez) için birim testi.

## Bitirme
`cd frontend && npm test && npm run build` (npm önbelleği hatası alırsan `npm ci --cache ./.npm-cache` dene ve
`.npm-cache`'i `frontend/.gitignore`'a ekle). Kısa rapor: dosyalar, test sonucu.
