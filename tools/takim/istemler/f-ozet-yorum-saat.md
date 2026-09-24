# Görev: Frontend — özet ve yorumlar paneli, açılış saati uyarısı

Önce `AGENTS.md` ve `docs/api-sozlesme.md` ("Yer detayı" ve "Aşama 8 eklemeleri") oku. Sadece `frontend/` altında çalış.
**Commit atma.** npm önbellek hatasında `npm ci --cache ./.npm-cache`.

## 1. Detay paneli (`YerDetayi.tsx`)
- **Özet:** açıklamanın yerine (varsa) `ozet.metin`; altında küçük gri yazı "Yapay zekâ ile Vikipedi'den özetlendi" ve
  `ozet.vikipedi` bağlantısı. `ozet` yoksa bugünkü `wikidata.aciklama` gösterilmeye devam eder.
- **Google yorumları:** puanın altında en fazla 3 kart: yazar adı (`yazarBaglantisi` varsa bağlantı, `guvenliSite` ile),
  yıldızlar, `zaman`, metin (4 satırdan uzunsa kısaltılır, "Devamı" ile açılır). Bölüm başlığında "Google yorumları".
- Tüm metinler düz metin (HTML olarak basma), tüm bağlantılar `guvenliSite` + `target="_blank" rel="noopener noreferrer"`.

## 2. Açılış saati uyarısı
- OSM `opening_hours` biçimini ayrıştırmak için hazır bir kütüphane kullan (npm'de lisansını ve bakımını kontrol et;
  MIT tercih). Ayrıştırılamayan ifade → uyarı YOK (yanlış uyarı vermektense sessiz kal).
- Saf fonksiyon `src/rota/acilisSaati.ts`: `durum(calismaSaatleri, varis: Date, kalisDakika)` →
  `'acik' | 'kapali' | 'kalisSirasindaKapaniyor' | 'bilinmiyor'` ve ilgili saat (açılış ya da kapanış).
- "Duraklarım" listesinde ve yolculuk modundaki durak kartlarında:
  - `kapali` → "Varışta kapalı olabilir (açılış 09:00)" (turuncu uyarı)
  - `kalisSirasindaKapaniyor` → "Kalışın sırasında kapanıyor (17:00)"
  - diğerleri → hiçbir şey.
- Tarih: zaman çizelgesi gece yarısını geçerse ertesi günün kuralı uygulanır (Date nesnesi zaten doğru günü taşır).
- Yolculuk modunda "Vardım" sonrası yeniden hesaplanan saatlerle uyarı da güncellenir.
- Durak verisinde `calismaSaatleri` yoksa (kayıtlı rotadan açılan eski duraklar dahil) uyarı yok.

## Testler (vitest)
- `acilisSaati`: `Mo-Su 09:00-17:00` (açık, kapalı, kalış sırasında kapanıyor), `Tu-Su 09:00-17:00` pazartesi kapalı,
  `24/7`, ayrıştırılamayan metin → `bilinmiyor`.
- Detay paneli: özet varken açıklama gizli; yorum yokken bölüm yok.

## Bitirme
`cd frontend && npm test && npm run build` hatasız; 390 px'te yatay taşma olmasın. Kısa rapor.
