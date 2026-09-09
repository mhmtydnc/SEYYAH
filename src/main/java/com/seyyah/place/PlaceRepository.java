package com.seyyah.place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PlaceRepository extends JpaRepository<Place,Long> {

@Query(value= """

    SELECT p.id, p.ad, p.kategori, p.tur,
        ST_Y(p.konum::geometry) AS enlem,
        ST_X(p.konum::geometry) AS boylam,
        ST_Distance(p.konum, r.hat)        AS yola_uzaklik,
        ST_LineLocatePoint(r.hat::geometry, p.konum::geometry)   AS yol_orani,
        p.ucret, p.calisma_saatleri, p.wikidata_id, p.website
        FROM places p,
        (SELECT ST_GeogFromText(:wkt) AS hat) r
        WHERE p.tur = :tur
        AND ST_DWithin(p.konum, r.hat,  :yaricap)
        ORDER BY yol_orani
        LIMIT :limit  
""", nativeQuery = true)

    List<Object[]> koridorda(@Param("wkt")String rotaWkt,
                             @Param("tur") String tur,
                             @Param("yaricap")double yaricap,
                             @Param ("limit") int limit);

}
