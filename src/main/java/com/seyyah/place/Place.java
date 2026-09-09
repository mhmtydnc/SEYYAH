package com.seyyah.place;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.time.OffsetDateTime;

@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "osm_type", length = 1, nullable = false)
    private String osmType;


    public String getOsmType() {
        return osmType;
    }

    public void setOsmType(String osmType) {
        this.osmType = osmType;
    }

    @Column(name = "osm_id", nullable = false)
    private Long osmId;
    public Long getOsmId() {
        return osmId;
    }

    public void setOsmId(Long osmId) {
        this.osmId = osmId;
    }

    @Column(nullable = false)
    private String ad;

    public String getAd() {
        return ad;
    }

    public void setAd(String ad) {
        this.ad = ad;
    }

    @Column(nullable = false)
    private String kategori;

    public String getKategori() {
        return kategori;
    }

    public void setKategori(String kategori) {
        this.kategori = kategori;
    }

    @Column(nullable = false)
    private String tur;

    public String getTur() {
        return tur;
    }

    public void setTur(String tur) {
        this.tur = tur;
    }


    @Column(name = "konum", columnDefinition = "geography(Point,4326)", nullable = false)
    private Point konum;
    public Point getKonum() {
        return konum;
    }

    public void setKonum(Point konum) {
        this.konum = konum;
    }

    private String ucret;

    public String getUcret() {
        return ucret;
    }

    public void setUcret(String ucret) {
        this.ucret = ucret;
    }

    @Column(name = "calisma_saatleri")
    private String calismaSaatleri;

    public String getCalismaSaatleri() {
        return calismaSaatleri;
    }

    public void setCalismaSaatleri(String calismaSaatleri) {
        this.calismaSaatleri = calismaSaatleri;
    }

    @Column(name = "wikidata_id")
    private String wikidataId;

    public String getWikidataId() {
        return wikidataId;
    }

    public void setWikidataId(String wikidataId) {
        this.wikidataId = wikidataId;
    }


    private String website;

    public String getWebsite() {
        return website;
    }
    public void setWebsite(String website) {
        this.website = website;
    }

    @Column(name = "onem_skoru", nullable = false)
    private Integer onemSkoru = 0;

    public Integer getOnemSkoru() {
        return onemSkoru;
    }

    public void setOnemSkoru(Integer onemSkoru) {
        this.onemSkoru = onemSkoru;
    }
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }


}