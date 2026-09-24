package com.seyyah.place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PlaceRepository extends JpaRepository<Place,Long> {

    // Rota, limit/2 bölüme ayrılır: her bölüme ortalama iki yer düşer, rotanın uzunluğundan bağımsız
    default List<KoridorYeri> koridorda(String rotaWkt, String tur, double yaricap, int limit) {
        return koridordaBolumlu(rotaWkt, tur, yaricap, limit, Math.max(1, (limit + 1) / 2));
    }

@Query(value= """

    WITH rota AS (
        -- ~50 m tolerans: yarıçap en az 100 m, sonuca etkisi yok ama nokta sayısı çok düşer.
        -- SRID içermeyen WKT de kabul edilsin diye 4326 zorlanır.
        -- DİKKAT: bu yorumlarda tek tırnak kullanma; Spring Data yorumları tanımaz, metin başlangıcı sanır.
        SELECT ST_SimplifyPreserveTopology(ST_SetSRID(ST_GeomFromEWKT(:wkt), 4326), 0.0005) AS hat
    ),

        parcalar AS (
            -- Rota, bolum sinirlariyla hizali esit parcalara bolunur (bolum basina 4). Kutulari dar oldugu icin
            -- GIST indeksi isler; her parcanin rota uzerindeki araligi da bilindiginden adayin bolumu en yakin
            -- parcasindan gelir. Binlerce aday icin ST_LineLocatePoint hesaplamak 700 km lik rotada saniyeler suruyordu.
            SELECT i AS parca_no,
                   ST_LineSubstring(rota.hat, i::float8 / (4 * :bolumSayisi), (i + 1)::float8 / (4 * :bolumSayisi))::geography AS parca
            FROM rota, generate_series(0, 4 * :bolumSayisi - 1) AS i
        ),

        adaylar AS (
            -- Her aday icin en yakin parca: ona uzaklik = rotaya uzaklik, sirasi = bolumu
            SELECT DISTINCT ON (p.id) p.id, ST_Distance(p.konum, pr.parca) AS uzaklik, pr.parca_no
            FROM places p
            JOIN parcalar pr ON ST_DWithin(p.konum, pr.parca, :yaricap)
            WHERE p.tur = :tur
            ORDER BY p.id, ST_Distance(p.konum, pr.parca)
        ),

        sirali AS (
            SELECT p.*, a.uzaklik,
                   ROW_NUMBER() OVER (
                       PARTITION BY a.parca_no / 4
                       ORDER BY p.onem_skoru DESC, a.uzaklik
                   ) AS bolum_sirasi
            FROM adaylar a
            JOIN places p ON p.id = a.id
        ),

        secilen AS (
            -- Sırayla seçim: önce her bölümün en iyisi, sonra ikincisi... Boş bölümlerin
            -- payını dolu bölümlerin sonraki yerleri alır.
            SELECT *
            FROM sirali
            ORDER BY bolum_sirasi, onem_skoru DESC, uzaklik
            LIMIT :limit
        )

    SELECT s.id, s.ad, s.kategori, s.tur,
        ST_Y(s.konum::geometry)        AS enlem,
        ST_X(s.konum::geometry)        AS boylam,
        ROUND(s.uzaklik)::int          AS "yolaUzaklikM",
        ST_LineLocatePoint(rota.hat, s.konum::geometry) AS "yolOrani",
        s.ucret,
        s.calisma_saatleri             AS "calismaSaatleri",
        s.wikidata_id                  AS "wikidataId",
        s.website
    FROM secilen s
    CROSS JOIN rota
    ORDER BY "yolOrani"

""", nativeQuery = true)

    List<KoridorYeri> koridordaBolumlu(@Param("wkt") String rotaWkt,
                                       @Param("tur") String tur,
                                       @Param("yaricap") double yaricap,
                                       @Param("limit") int limit,
                                       @Param("bolumSayisi") int bolumSayisi);

}
