package com.seyyah.detay;

public record GoogleDetay(
        Double puan,
        Integer yorumSayisi,
        String haritaBaglantisi,
        java.util.List<GoogleYorum> yorumlar
) {}
