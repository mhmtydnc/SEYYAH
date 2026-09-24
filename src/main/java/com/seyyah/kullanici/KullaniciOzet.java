package com.seyyah.kullanici;

public record KullaniciOzet(Long id, String ad, String eposta) {

    public static KullaniciOzet olustur(Kullanici kullanici) {
        return new KullaniciOzet(kullanici.getId(), kullanici.getAd(), kullanici.getEposta());
    }
}
