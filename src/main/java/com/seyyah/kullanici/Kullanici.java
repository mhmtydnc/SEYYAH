package com.seyyah.kullanici;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "kullanicilar")
public class Kullanici {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ad;

    @Column(nullable = false, unique = true)
    private String eposta;

    @Column(name = "sifre_ozeti", nullable = false)
    private String sifreOzeti;

    @CreationTimestamp
    @Column(name = "olusturulma", nullable = false, updatable = false)
    private OffsetDateTime olusturulma;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAd() {
        return ad;
    }

    public void setAd(String ad) {
        this.ad = ad;
    }

    public String getEposta() {
        return eposta;
    }

    public void setEposta(String eposta) {
        this.eposta = eposta;
    }

    public String getSifreOzeti() {
        return sifreOzeti;
    }

    public void setSifreOzeti(String sifreOzeti) {
        this.sifreOzeti = sifreOzeti;
    }

    public OffsetDateTime getOlusturulma() {
        return olusturulma;
    }
}
