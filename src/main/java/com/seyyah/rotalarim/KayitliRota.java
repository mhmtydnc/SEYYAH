package com.seyyah.rotalarim;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "kayitli_rotalar")
public class KayitliRota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kullanici_id", nullable = false)
    private Long kullaniciId;

    @Column(nullable = false)
    private String baslik;

    @Column(name = "kalkis_ad", nullable = false)
    private String kalkisAd;

    @Column(name = "kalkis_enlem", nullable = false)
    private Double kalkisEnlem;

    @Column(name = "kalkis_boylam", nullable = false)
    private Double kalkisBoylam;

    @Column(name = "varis_ad", nullable = false)
    private String varisAd;

    @Column(name = "varis_enlem", nullable = false)
    private Double varisEnlem;

    @Column(name = "varis_boylam", nullable = false)
    private Double varisBoylam;

    @CreationTimestamp
    @Column(name = "olusturulma", nullable = false, updatable = false)
    private OffsetDateTime olusturulma;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKullaniciId() {
        return kullaniciId;
    }

    public void setKullaniciId(Long kullaniciId) {
        this.kullaniciId = kullaniciId;
    }

    public String getBaslik() {
        return baslik;
    }

    public void setBaslik(String baslik) {
        this.baslik = baslik;
    }

    public String getKalkisAd() {
        return kalkisAd;
    }

    public void setKalkisAd(String kalkisAd) {
        this.kalkisAd = kalkisAd;
    }

    public Double getKalkisEnlem() {
        return kalkisEnlem;
    }

    public void setKalkisEnlem(Double kalkisEnlem) {
        this.kalkisEnlem = kalkisEnlem;
    }

    public Double getKalkisBoylam() {
        return kalkisBoylam;
    }

    public void setKalkisBoylam(Double kalkisBoylam) {
        this.kalkisBoylam = kalkisBoylam;
    }

    public String getVarisAd() {
        return varisAd;
    }

    public void setVarisAd(String varisAd) {
        this.varisAd = varisAd;
    }

    public Double getVarisEnlem() {
        return varisEnlem;
    }

    public void setVarisEnlem(Double varisEnlem) {
        this.varisEnlem = varisEnlem;
    }

    public Double getVarisBoylam() {
        return varisBoylam;
    }

    public void setVarisBoylam(Double varisBoylam) {
        this.varisBoylam = varisBoylam;
    }

    public OffsetDateTime getOlusturulma() {
        return olusturulma;
    }
}
