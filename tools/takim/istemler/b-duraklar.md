# Görev: Backend — duraklı rota, ara nokta bilgisi, kayıtlı rotada duraklar

Önce `AGENTS.md` ve `docs/api-sozlesme.md` dosyalarını oku. Uygulayacağın bölümler:
"Rota — Aşama 5" altındaki **Aşama 6 `araNokta`** notu, **"Duraklı rota — Aşama 6"** ve
"Kayıtlı rotalar" altındaki **Aşama 6 alanları** (`uzerinden`, `duraklar`). Sözleşmeye BİREBİR uy.
**Commit atma**, değişiklikleri çalışma ağacında bırak.

İncele: `src/main/java/com/seyyah/route/*` (özellikle `OpenRouteService` — koordinat listesi alan özel
metot zaten var, `AlternatifRotaServisi`, `RotaController`, `RotaYaniti`), `src/main/java/com/seyyah/rotalarim/*`,
`src/main/resources/db/migration/V4__kayitli_rotalar.sql`, ilgili testler.

## 1. `araNokta`
`RotaYaniti.Rota`'ya `araNokta` (ad, enlem, boylam; yoksa null) ekle. "X üzerinden" rotalarda
`AlternatifRotaServisi`'nin kullandığı aday şehrin koordinatı. Diğer rotalarda null.

## 2. `POST /api/rota/duraklu`
- Gövde: `noktalar` listesi (2–12), her biri `enlem` (-90..90), `boylam` (-180..180), isteğe bağlı `durakId`.
  Doğrulama `@Valid` + `@Size` ile; geçersizse 400 (mevcut ProblemDetail yapısıyla).
- `OpenRouteService`'e çok noktalı rota metodu ekle (her nokta için `radiuses` 5000). ORS yanıtında
  `features[0].properties.segments[]` her bacak için `distance`/`duration` içerir → `bacaklar`.
- Yanıt: `mesafeM`, `sureSn`, `geometri`, `bacaklar`. Koridor/yer sorgusu **yapma** (istemcide zaten var).
- Güvenlik: herkese açık (GuvenlikAyari'nda `/api/rota/**` zaten permitAll mı kontrol et).

## 3. Kayıtlı rotada `uzerinden` ve `duraklar`
- `V6__kayitli_rota_duraklar.sql`: `kayitli_rotalar`'a `uzerinden JSONB NULL`, `duraklar JSONB NOT NULL DEFAULT '[]'`.
- Entity'de Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)` ile (record tipleri: `AraNokta(ad, enlem, boylam)`,
  `Durak(id, ad, kategori, enlem, boylam)`). `ddl-auto: validate` ile uyumlu olmalı.
- `POST /api/rotalarim` bu alanları isteğe bağlı kabul etsin (en fazla 10 durak, `@Size`), GET ve POST yanıtında dönsün.
  Eski istemci (alanlar yok) çalışmaya devam etmeli.

## Testler
- `OpenRouteServiceTest`: çok noktalı istek gövdesi (koordinatlar sırası + radiuses) ve `segments` ayrıştırma.
- Duraklı rota controller testi (WebMvcTest, mevcutlar gibi `@Import(GuvenlikAyari.class)` +
  `@TestPropertySource(jwt.gizli=...)`): başarılı, 1 nokta → 400, 13 nokta → 400.
- `RotaControllerTest`: "X üzerinden" rotada `araNokta` dolu, ana rotada null.
- `KayitliRotaControllerTest`: duraklı kayıt ve alanlar olmadan eski biçimde kayıt.
- `KayitliRotaRepositoryTest` (Testcontainers; yerelde SKIP normal): JSONB alanlarının yazılıp okunması.

## Bitirme
`JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2026.2.3/jbr" ./mvnw -q test` hatasız (DB testleri SKIP).
Kısa rapor: dosyalar, test sonucu, belirsiz kararlar.
