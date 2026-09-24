# Görev: opening_hours kütüphanesini ihtiyaç anında yükle

Önce `AGENTS.md`'yi oku. Sadece `frontend/` altında çalış. **Commit atma.**
npm önbellek hatasında `npm ci --cache ./.npm-cache`.

## Sorun
`src/rota/acilisSaati.ts` `opening_hours`'ı statik import ediyor; ana paket 434 KB'tan 1.173 KB'a çıktı (tüm ülkelerin tatil
verisi). Kütüphane yalnızca durak eklenince ve `calismaSaatleri` dolu bir durak varken gerekiyor.

## Yapılacak
- `acilisSaati.ts`: statik import yerine `import('opening_hours')` ile tembel yükleme. Modül düzeyinde yükleme sözü (promise)
  tek kez oluşturulsun. `durum(...)` senkron kalsın: kütüphane yüklenmemişse `'bilinmiyor'` döner.
- `acilisSaatiHazir(): boolean` ve `acilisSaatiYukle(): Promise<void>` dışa açılsın.
- `AnaSayfa` ve `YolculukSayfasi`: `calismaSaatleri` dolu en az bir durak/nokta varsa `useEffect` içinde
  `acilisSaatiYukle()` çağır, bitince yeniden çizim tetikle (ör. bir `useState` sayacı). Yoksa hiç yükleme.
- Testler: mevcut `acilisSaati.test.ts` testlerinde önce `await acilisSaatiYukle()` çağır; ayrıca yüklenmeden önce
  `durum` → `'bilinmiyor'` testi ekle.

## Kabul
`npm run build` çıktısında ana `index-*.js` 500 KB'ın altında olmalı; `opening_hours` ayrı bir parça (chunk) olarak
görünmeli. `npm test` ve `npm run build` hatasız. Build çıktısındaki dosya boyutlarını rapora yaz.
