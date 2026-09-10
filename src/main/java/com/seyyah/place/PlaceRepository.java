package com.seyyah.place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PlaceRepository extends JpaRepository<Place,Long> {

@Query(value= """

    WITH rota AS(
        SELECT ST_GeogFromText(:wkt) AS hat
    ),
        
        adaylar AS (
            SELECT p.*, ST_Distance(p.konum, rota.hat) AS uzaklik
            FROM places p , rota
            WHERE p.tur = :tur
            AND ST_DWithin(p.konum, rota.hat, :yaricap)
            ORDER BY p.onem_skoru DESC, uzaklik
            LIMIT :limit
        )

    SELECT a.id, a.ad, a.kategori, a.tur,
        ST_Y(a.konum::geometry) AS enlem,
        ST_X(a.konum::geometry) AS boylam,
        Round(a.uzaklik)::int        AS "yolaUzaklikM",
        ST_LineLocatePoint(rota.hat::geometry, a.konum::geometry)   AS "yolOrani",
        a.ucret , 
        a.calisma_saatleri             AS "calismaSaatleri", 
        a.wikidata_id                  AS "wikidataId", 
        a.website
    FROM adaylar a, rota
    ORDER BY "yolOrani"
       
""", nativeQuery = true)

    List<KoridorYeri> koridorda(@Param("wkt")String rotaWkt,
                             @Param("tur") String tur,
                             @Param("yaricap")double yaricap,
                             @Param ("limit") int limit);

}
