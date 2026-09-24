package com.seyyah.detay;

import com.seyyah.hata.ApiIstisnasi;
import com.seyyah.place.Place;
import com.seyyah.place.PlaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

@Service
public class SoruServisi {

    private final PlaceRepository placeRepository;
    private final GeminiIstemcisi geminiIstemcisi;
    private final VikipediMetni vikipediMetni;
    private final JdbcClient jdbcClient;
    private final WikidataServisi wikidataServisi;

    public SoruServisi(PlaceRepository placeRepository,
                       GeminiIstemcisi geminiIstemcisi,
                       VikipediMetni vikipediMetni,
                       JdbcClient jdbcClient,
                       WikidataServisi wikidataServisi) {
        this.placeRepository = placeRepository;
        this.geminiIstemcisi = geminiIstemcisi;
        this.vikipediMetni = vikipediMetni;
        this.jdbcClient = jdbcClient;
        this.wikidataServisi = wikidataServisi;
    }

    @Transactional
    public SoruYaniti hazirSoru(Long id, String soruKodu) {
        String soruMetni = switch (soruKodu) {
            case "deger" -> "Görmeye değer mi?";
            case "sure" -> "Ne kadar zaman ayırmalıyım?";
            case "cocuk" -> "Çocuklarla uygun mu?";
            case "ipucu" -> "Ziyaret için ipuçları";
            default -> throw new ApiIstisnasi(HttpStatus.BAD_REQUEST, "Geçersiz hazır soru");
        };

        // Önbellek kontrolü
        String cacheSql = "SELECT cevap FROM yer_soru_onbellek WHERE yer_id = ? AND soru = ?";
        String onbellekCevap = jdbcClient.sql(cacheSql)
                .param(id)
                .param(soruKodu)
                .query(String.class)
                .optional()
                .orElse(null);

        if (onbellekCevap != null) {
            return new SoruYaniti(onbellekCevap, true);
        }

        String cevap = soruUret(id, soruMetni);

        // Önbelleğe yaz
        String insertSql = """
                INSERT INTO yer_soru_onbellek (yer_id, soru, cevap)
                VALUES (?, ?, ?)
                ON CONFLICT (yer_id, soru) DO NOTHING
                """;
        jdbcClient.sql(insertSql)
                .param(id)
                .param(soruKodu)
                .param(cevap)
                .update();

        return new SoruYaniti(cevap, false);
    }

    public SoruYaniti serbestSoru(Long id, String metin, Long kullaniciId) {
        if (metin == null || metin.length() < 3 || metin.length() > 200) {
            throw new ApiIstisnasi(HttpStatus.BAD_REQUEST, "Soru metni 3-200 karakter olmalıdır");
        }

        // Kişisel saatlik sınır (10)
        String saat = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH"));
        String servisAdi = "soru-uye-" + kullaniciId;
        
        String sqlKisisel = """
                INSERT INTO api_kullanim (ay, servis, sayi)
                VALUES (?, ?, 1)
                ON CONFLICT (ay, servis)
                DO UPDATE SET sayi = api_kullanim.sayi + 1
                RETURNING sayi
                """;
        Integer kisiSayi = jdbcClient.sql(sqlKisisel)
                .param(saat)
                .param(servisAdi)
                .query(Integer.class)
                .single();
                
        if (kisiSayi != null && kisiSayi > 10) {
            throw new ApiIstisnasi(HttpStatus.TOO_MANY_REQUESTS, "Saatlik soru sınırını aştınız");
        }

        String cevap = soruUret(id, metin);
        return new SoruYaniti(cevap, false);
    }

    private String soruUret(Long id, String soru) {
        if (!geminiIstemcisi.etkin()) {
            throw new ApiIstisnasi(HttpStatus.SERVICE_UNAVAILABLE, "Yapay zekâ şu an yanıt veremiyor, biraz sonra tekrar deneyin.");
        }

        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new ApiIstisnasi(HttpStatus.NOT_FOUND, "Yer bulunamadı"));

        // Günlük kota kontrolü (300)
        String gun = LocalDate.now().toString();
        String sqlKota = """
                INSERT INTO api_kullanim (ay, servis, sayi)
                VALUES (?, 'gemini', 1)
                ON CONFLICT (ay, servis)
                DO UPDATE SET sayi = api_kullanim.sayi + 1
                RETURNING sayi
                """;
        Integer sayi = jdbcClient.sql(sqlKota)
                .param(gun)
                .query(Integer.class)
                .single();
                
        if (sayi == null || sayi > 300) {
            throw new ApiIstisnasi(HttpStatus.TOO_MANY_REQUESTS, "Günlük yapay zekâ kotası doldu");
        }

        StringBuilder bilgiler = new StringBuilder();
        bilgiler.append("Ad: ").append(place.getAd()).append("\n");
        if (place.getKategori() != null) bilgiler.append("Kategori: ").append(place.getKategori()).append("\n");
        if (place.getTur() != null) bilgiler.append("Tür: ").append(place.getTur()).append("\n");
        if (place.getCalismaSaatleri() != null) bilgiler.append("Çalışma Saatleri: ").append(place.getCalismaSaatleri()).append("\n");
        if (place.getUcret() != null) bilgiler.append("Ücret: ").append(place.getUcret()).append("\n");
        // "Ne kadar zaman" sorusu uydurmaya değil bizim verimize dayansın (arayüzdeki varsayılan kalış süresiyle aynı)
        bilgiler.append("Kategoriye göre kaba ziyaret süresi tahmini: yaklaşık ")
                .append(TipikZiyaret.dakika(place.getKategori())).append(" dakika\n");

        if (place.getWikidataId() != null) {
            String vikiUrl = getVikiUrl(place.getWikidataId());
            if (vikiUrl != null) {
                try {
                    String vikiMetin = vikipediMetni.metinGetir(vikiUrl);
                    if (vikiMetin != null) {
                        if (vikiMetin.length() > 3000) {
                            vikiMetin = vikiMetin.substring(0, 3000);
                        }
                        bilgiler.append("Vikipedi: ").append(vikiMetin).append("\n");
                    }
                } catch (Exception e) {
                    // Ignore wiki errors for questions
                }
            }
        }

        // İlk sürüm yalnızca kesin bilgiye izin veriyordu; "ne kadar zaman", "çocuklarla uygun mu" gibi sorulara
        // hep "bilgi yok" diyordu. Çıkarım serbest, yeni olgu (tarih, fiyat, saat, kural) uydurmak yasak.
        String sistemTalimati = """
                Sen Seyyah uygulamasında bir gezi rehberisin. Soruyu 3-4 cümleyle, Türkçe ve samimi ama abartısız yanıtla.
                Yanıtını verilen BİLGİLER'e dayandır. Bilgilerden makul çıkarımlar yapabilirsin (ör. yerin türünden ne
                beklenebileceği, kimlere uygun olabileceği); kesin olmayan yerde "genellikle", "büyük ihtimalle" gibi
                temkinli bir dil kullan. BİLGİLER'de olmayan yeni olgu uydurma: tarih, fiyat, açılış saati, kural,
                etkinlik ya da isim ekleme. Ziyaret süresi sorulursa kategoriye göre verilen kaba tahmini başlangıç
                noktası al; BİLGİLER yerin büyük bir kompleks olduğunu, müze ya da birden çok yapı içerdiğini
                gösteriyorsa daha uzun sürebileceğini söyle ve makul bir aralık ver; tahminin nereden geldiğini
                (kategori, kaba tahmin) kullanıcıya anlatma. Soru bu yerle ilgili
                değilse kibarca yalnızca bu yer hakkında yardım edebileceğini söyle. Kişisel veri isteme, tıbbi ya da
                hukuki tavsiye verme.""";
        String kullaniciMesaji = "BİLGİLER:\n" + bilgiler.toString() + "\n\nSORU: " + soru;

        try {
            String cevap = geminiIstemcisi.icerikUret(sistemTalimati, kullaniciMesaji);
            cevap = cevap.replaceAll("[*#_]", "");
            if (cevap.length() > 700) {
                cevap = cevap.substring(0, 700);
            }
            return cevap.trim();
        } catch (Exception e) {
            throw new ApiIstisnasi(HttpStatus.SERVICE_UNAVAILABLE, "Yapay zekâ şu an yanıt veremiyor, biraz sonra tekrar deneyin.");
        }
    }

    private String getVikiUrl(String wikidataId) {
        String sql = "SELECT wikidata->>'vikipedi' FROM yer_detay_onbellek WHERE yer_id = (SELECT id FROM places WHERE wikidata_id = ? LIMIT 1)";
        String vUrl = jdbcClient.sql(sql)
                .param(wikidataId)
                .query(String.class)
                .optional()
                .orElse(null);
        if (vUrl != null) return vUrl;
        
        WikidataDetay detay = wikidataServisi.detayGetir(wikidataId);
        return detay != null ? detay.vikipedi() : null;
    }
}
