# Görev: Frontend — detay panelinde "Bu yer hakkında sor"

Önce `AGENTS.md` ve `docs/api-sozlesme.md` ("Yere soru sor — Aşama 9") oku. Sadece `frontend/` altında çalış. **Commit atma.**
npm önbellek hatasında `npm ci --cache ./.npm-cache`.

## Yapılacak (`YerDetayi.tsx` içinde, özetin altında yeni bölüm)
- Başlık "Bu yer hakkında sor". Dört düğme (chip): "Görmeye değer mi?" (`deger`), "Ne kadar zaman ayırmalıyım?" (`sure`),
  "Çocuklarla uygun mu?" (`cocuk`), "Ziyaret için ipuçları" (`ipucu`) → `POST /api/yerler/{id}/hazir-soru`.
- Giriş yapılmışsa altta metin kutusu (en fazla 200 karakter, sayaç) + "Sor" → `POST /api/uye/yerler/{id}/soru`.
  Giriş yoksa kutu yerine "Kendi sorunu sormak için giriş yap" bağlantısı (`/giris`).
- Yanıtlar soru-cevap listesi olarak (en yeni üstte) panelde kalır; yer değişince temizlenir. Aynı hazır soru ikinci kez
  basılınca yeniden istek atılmaz (bellek içi önbellek).
- Yüklenirken ilgili düğme pasif + "Düşünüyor…". Hatalar: 429 → "Çok fazla soru sordun, biraz sonra tekrar dene",
  503 → "Yapay zekâ şu an yanıt veremiyor", diğerleri ProblemDetail `detail`.
- Bölümün altında küçük gri not: "Yanıtlar yapay zekâ (Google Gemini) tarafından üretilir, hata içerebilir.
  Sorun Google'a iletilir."
- Cevap düz metin (HTML olarak basma). 390 px'te taşma olmasın, düğmeler satıra sığmazsa alt satıra geçsin.

## Test (vitest + testing-library)
- Hazır soru düğmesine basınca doğru uç çağrılır ve cevap görünür; ikinci basışta istek yok.
- Giriş yokken metin kutusu yok, giriş bağlantısı var.
- 429 yanıtında doğru mesaj.

## Bitirme
`cd frontend && npm test && npm run build` hatasız. Ana `index-*.js` 500 KB'ı geçmesin. Kısa rapor.
