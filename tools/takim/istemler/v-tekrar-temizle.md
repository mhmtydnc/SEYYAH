# Görev: Mükerrer POI temizleme aracı

Önce `AGENTS.md`, `src/main/resources/db/migration/V1__init.sql`, `V2__tur_kolonu.sql`, `V5__yerlesimler.sql` ve
`tools/osm/yerlesimleri_yukle.py` (bağlantı ve üslup örneği) dosyalarını oku. **Commit atma. Veritabanına bağlanma.**

## Sorun
`places` tablosunda aynı yer birden çok kez var: OSM'de hem node hem way olarak, ya da farklı kategoriyle
(ör. "Kapalı Çarşı" attraction + museum, 160 m arayla). Ölçüm: aynı `tur`, aynı sadeleştirilmiş ad
(`lower(f_unaccent(ad))`) ve 300 m içinde ~3.800 çift.

## Yapılacak: `tools/osm/tekrar_temizle.py`
- Bağlantı: `yerlesimleri_yukle.py`'deki gibi `DB_URL`/`DB_USER`/`DB_PASS` (sözlükle bağlan, sslmode).
- **Varsayılan kuru çalıştırma** (`--uygula` verilmedikçe hiçbir şey yazma): kaç küme, kaç satır silinecek, en
  önemli 20 kümenin özeti (ad, kategoriler, osm tipleri, aradaki mesafe) yazdırılır.
- Kümeleme SQL'de: aynı `tur` + aynı `lower(f_unaccent(ad))` + `ST_DWithin(konum, konum, 300)`. Zincirleme
  eşleşmeler (A~B, B~C) tek küme olsun (bağlı bileşenler; Python'da union-find yeterli).
- Her kümede **tutulacak kayıt**: en yüksek `onem_skoru`; eşitse way/relation (`w`,`r`) node'a tercih edilir;
  yine eşitse dolu alan (`wikidata_id`, `website`, `calisma_saatleri`, `ucret`) sayısı fazla olan; yine eşitse küçük `id`.
- Tutulan kayda, silinenlerde olup onda boş olan `wikidata_id`, `website`, `calisma_saatleri`, `ucret` alanları
  aktarılır; `onem_skoru` kümenin en yükseği olur.
- `--uygula`: hepsi **tek transaction**'da; sonunda `ANALYZE places`; kaç satır güncellendi/silindi yazdırılır.
- İsimsiz ya da çok kısa (<3 harf) adlar kümelemeye alınmaz ("Park", "Cami" gibi jenerik adlar için: ad 3 kelimeden
  kısa VE kategori `park`/`place_of_worship`/`parking`/`toilets` ise mesafe eşiği 60 m olsun — farklı yerleri
  birleştirmemek için).
- Kullanımı dosyanın başındaki docstring'de anlat (`donustur.py` üslubu).

## Test
`tools/osm/test_tekrar_temizle.py` (pytest ya da unittest, DB'siz): union-find kümeleme ve "tutulacak kayıt"
seçim kuralı saf fonksiyonlar olsun ve bunlar test edilsin. `python -m pytest tools/osm` ya da
`python -m unittest discover tools/osm` geçmeli.

Kısa rapor: dosyalar, test sonucu, belirsiz kalanlar.
