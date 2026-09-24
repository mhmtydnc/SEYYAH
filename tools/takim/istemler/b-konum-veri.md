# Görev: Konum araması kendi verimizden (yerleşimler + gezi yerleri), ORS yedek

Önce `AGENTS.md`, `docs/api-sozlesme.md` ("Yer arama") ve mevcut `com.seyyah.konum` paketini oku.
Yanıt biçimi (`KonumSonucu`: ad, etiket, enlem, boylam) ve endpoint **değişmeyecek**; frontend buna bağlı.

## 1. Şema — `V5__yerlesimler.sql` (tek yeni migration; mevcutlara dokunma)
- `f_unaccent(text)`: `public.unaccent('public.unaccent'::regdictionary, $1)` saran `IMMUTABLE PARALLEL SAFE STRICT`
  SQL fonksiyonu (indeks ifadelerinde kullanılabilsin diye).
- `yerlesimler` tablosu: id BIGSERIAL, osm_type CHAR(1), osm_id BIGINT, ad TEXT, tur TEXT (city/town/village/suburb/…),
  il TEXT NULL, nufus INT NULL, onem INT NOT NULL DEFAULT 0, konum geography(Point,4326), UNIQUE(osm_type, osm_id).
- İndeksler: `GIN (lower(f_unaccent(ad)) gin_trgm_ops)` hem `yerlesimler` hem `places` için, ayrıca `yerlesimler` GIST(konum).
- Yeni bir JPA entity gerekmez; native sorgu yeterli (ddl-auto validate etkilenmesin).

## 2. Arama sorgusu
Native SQL (Spring Data `@Query` ya da `JdbcClient`). Sorgu metni `lower(f_unaccent(:q))`.
- Önce yerleşimler, sonra `places` içinden sadece `tur = 'gezi'` olanlar (ör. "Anıtkabir" hedef olarak aranabilsin).
- Eşleşme: önek (`LIKE q || '%'`) ya da trigram benzerliği (`%` operatörü / `similarity`).
- Sıralama: önek eşleşmesi önce → yerleşimde `onem` (city > town > suburb > village) ve `nufus` → benzerlik → gezi yerleri en sonda.
- `etiket`: yerleşimde `"Ad, İl"` (il yoksa `"Ad (şehir|ilçe/kasaba|mahalle|köy)"`), gezi yerinde `"Ad (kategori)"`.
- SQL yorumlarında tek tırnak kullanma (Spring Data sorunu, bkz. `PlaceRepository`).

## 3. `KonumServisi` — karma
Önce kendi verimiz. **Hiç sonuç yoksa** mevcut ORS geocode çağrısına düş. ORS yedekte hata verirse boş liste dön
ve logla (kendi verimiz boşken ORS de çökmüşse kullanıcıya 5xx göstermenin anlamı yok). Mevcut ORS kodunu sil**me**.

## 4. Veri çıkarma — `tools/osm/yerlesimler.py`
`pyosmium` (`pip install osmium`) ile `veri/turkey-latest.osm.pbf`'den `place=city,town,village,suburb,quarter,neighbourhood`
node'larını okuyup `veri/yerlesimler.csv` üretir: osm_type, osm_id, ad (`name`, yoksa `name:tr`), tur, il
(`is_in:province` / `addr:province` / `is_in` içinden ilk parça, yoksa boş), nufus (`population` sayıya çevrilebiliyorsa),
onem (city 100, town 60, suburb 30, quarter 20, village 10, neighbourhood 5), enlem, boylam.
Mevcut `donustur.py` üslubunu izle. Kullanım bilgisini dosyanın başına yaz.

## 5. Yükleme — `tools/osm/yerlesimleri_yukle.py`
`psycopg` (v3) ile `DB_URL`/`DB_USER`/`DB_PASS` ortam değişkenlerinden bağlanır (JDBC URL'sini ayrıştır),
CSV'yi geçici tabloya `COPY` eder, `INSERT … ON CONFLICT (osm_type, osm_id) DO UPDATE` ile `yerlesimler`'e aktarır,
eklenen/güncellenen sayısını yazar. Tek transaction. Şifreyi asla loglama.

## Testler
- `KonumServisi` birim testi: kendi veri doluysa ORS çağrılmaz; boşsa ORS çağrılır; ORS hata verirse boş liste.
- Repository sorgusu için DB testi **yazma** (Testcontainers altyapısını başka ajan kuruyor; lider sonra ekleyecek).

## Dokunma
`pom.xml`, `src/main/resources/application*.yaml`, güvenlik, `frontend/`, `deploy/`, `.github/`.
**PBF indirme, veritabanına bağlanma veya veri yükleme yapma** — bunu lider yapacak.

## Bitirme
`JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2026.2.3/jbr" ./mvnw -q test` (`contextLoads` hariç her şey geçmeli),
`python -m py_compile tools/osm/yerlesimler.py tools/osm/yerlesimleri_yukle.py`. Türkçe commit, kısa rapor.
