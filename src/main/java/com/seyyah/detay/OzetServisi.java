package com.seyyah.detay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OzetServisi {
    private static final Logger log = LoggerFactory.getLogger(OzetServisi.class);
    private final GeminiIstemcisi geminiIstemcisi;
    private final VikipediMetni vikipediMetni;
    private final JdbcClient jdbcClient;

    public OzetServisi(GeminiIstemcisi geminiIstemcisi,
                       VikipediMetni vikipediMetni,
                       JdbcClient jdbcClient) {
        this.geminiIstemcisi = geminiIstemcisi;
        this.vikipediMetni = vikipediMetni;
        this.jdbcClient = jdbcClient;
    }

    // Anahtar yoksa servis kapalıdır; çağıran bunu "özet yok" sanıp süresiz önbelleğe yazmamalı
    public boolean etkin() {
        return geminiIstemcisi.etkin();
    }

    public OzetDetay ozetUret(WikidataDetay wikidata) {
        if (!etkin()) {
            return null;
        }
        if (wikidata == null || wikidata.vikipedi() == null || wikidata.vikipedi().isBlank()) {
            return null;
        }

        String vikiUrl = wikidata.vikipedi();
        String extract = vikipediMetni.metinGetir(vikiUrl);

        if (extract == null) {
            return null;
        }

        String metin = extract;
        if (metin.length() > 2000) {
            metin = metin.substring(0, 2000);
        }

        if (!kotaArtirVeKontrolEt()) {
            throw new RuntimeException("Gemini quota error");
        }

        try {
            String istem = "Aşağıdaki Vikipedi metnine dayanarak bir gezgin için 2-3 cümlelik, Türkçe, abartısız bir tanıtım yaz: yerin ne olduğu ve neden görülmeye değer olduğu. Metinde olmayan bilgi ekleme.\n\nMETİN:\n" + metin;

            String sonuc = geminiIstemcisi.icerikUret(null, istem);

            sonuc = sonuc.replaceAll("[*#_]", "");
            if (sonuc.length() > 600) {
                sonuc = sonuc.substring(0, 600);
            }

            return new OzetDetay(sonuc.trim(), vikiUrl);
        } catch (Exception e) {
            log.warn("Gemini hatası: {}", e.getMessage());
            throw new RuntimeException("Gemini error", e);
        }
    }

    private boolean kotaArtirVeKontrolEt() {
        String gun = LocalDate.now().toString();
        String sql = """
                INSERT INTO api_kullanim (ay, servis, sayi)
                VALUES (?, 'gemini', 1)
                ON CONFLICT (ay, servis)
                DO UPDATE SET sayi = api_kullanim.sayi + 1
                RETURNING sayi
                """;
        Integer sayi = jdbcClient.sql(sql)
                .param(gun)
                .query(Integer.class)
                .single();
        
        return sayi != null && sayi <= 300;
    }
}
