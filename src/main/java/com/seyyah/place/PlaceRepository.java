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
        -- SRID'siz WKT de kabul edilsin diye 4326 zorlanır.
        SELECT ST_SimplifyPreserveTopology(ST_SetSRID(ST_GeomFromEWKT(:wkt), 4326), 0.0005) AS hat
    ),

        parcalar AS (
            -- Uzun, çapraz bir rotanın tek sınırlayıcı kutusu yarım ülkeyi kapsar ve GIST indeksi
            -- işe yaramaz. Küçük parçaların kutuları dardır; indeks her biri için az aday döndürür.
            SELECT ST_Subdivide(hat, 32)::geography AS parca
            FROM rota
        ),

        adaylar AS (
            -- Parçaların birleşimi rotanın kendisi: en yakın parçaya uzaklık = rotaya uzaklık
            SELECT p.id, MIN(ST_Distance(p.konum, pr.parca)) AS uzaklik
            FROM places p
            JOIN parcalar pr ON ST_DWithin(p.konum, pr.parca, :yaricap)
            WHERE p.tur = :tur
            GROUP BY p.id
        ),

        konumlu AS (
            SELECT p.*, a.uzaklik,
                   ST_LineLocatePoint(rota.hat, p.konum::geometry) AS oran
            FROM adaylar a
            JOIN places p ON p.id = a.id
            CROSS JOIN rota
        ),

        sirali AS (
            -- oran = 1 (rotanın son noktası) son bölüme düşsün diye LEAST
            SELECT k.*,
                   ROW_NUMBER() OVER (
                       PARTITION BY LEAST(FLOOR(k.oran * :bolumSayisi), :bolumSayisi - 1)
                       ORDER BY k.onem_skoru DESC, k.uzaklik
                   ) AS bolum_sirasi
            FROM konumlu k
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
        s.oran                         AS "yolOrani",
        s.ucret,
        s.calisma_saatleri             AS "calismaSaatleri",
        s.wikidata_id                  AS "wikidataId",
        s.website
    FROM secilen s
    ORDER BY s.oran

""", nativeQuery = true)

    List<KoridorYeri> koridordaBolumlu(@Param("wkt") String rotaWkt,
                                       @Param("tur") String tur,
                                       @Param("yaricap") double yaricap,
                                       @Param("limit") int limit,
                                       @Param("bolumSayisi") int bolumSayisi);

}
