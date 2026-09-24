package com.seyyah.detay;

import com.seyyah.hata.ApiIstisnasi;
import com.seyyah.place.Place;
import com.seyyah.place.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SoruServisiTest {
    private JdbcClient jdbcClient;
    private PlaceRepository placeRepository;
    private WikidataServisi wikidataServisi;

    @BeforeEach
    void setup() {
        jdbcClient = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        placeRepository = mock(PlaceRepository.class);
        wikidataServisi = mock(WikidataServisi.class);
        
        when(jdbcClient.sql(anyString()).param(any()).query(String.class).optional()).thenReturn(Optional.empty());
        when(jdbcClient.sql(anyString()).param(any()).query(Integer.class).single()).thenReturn(1);
        when(jdbcClient.sql(anyString()).param(any()).param(any()).param(any()).update()).thenReturn(1);
    }

    private SoruServisi createService(RestClient.Builder builder, String key) {
        GeminiIstemcisi gemini = new GeminiIstemcisi(builder, key, "gemini-flash-lite-latest");
        VikipediMetni wiki = new VikipediMetni(builder);
        return new SoruServisi(placeRepository, gemini, wiki, jdbcClient, wikidataServisi);
    }

    @Test
    void basariliSoru_MarkdownTemizligi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        SoruServisi soruServisi = createService(builder, "test-key");
        
        GeometryFactory gf = new GeometryFactory();
        Place p = new Place();
        // Since we cannot set ID via setter if there is no setter, let's just mock Place
        p = mock(Place.class);
        when(p.getId()).thenReturn(1L);
        when(p.getAd()).thenReturn("Anıtkabir");
        when(p.getKategori()).thenReturn("museum");
        when(p.getTur()).thenReturn("gezi");
        when(p.getKonum()).thenReturn(gf.createPoint(new Coordinate(32.8, 39.9)));
        when(p.getCalismaSaatleri()).thenReturn("09:00-17:00");
        when(p.getWikidataId()).thenReturn("Q1");
        when(placeRepository.findById(1L)).thenReturn(Optional.of(p));
        when(wikidataServisi.detayGetir("Q1")).thenReturn(new WikidataDetay("Mezar", "https://tr.wikipedia.org/wiki/An%C4%B1tkabir", null));
        
        mockServer.expect(MockRestRequestMatchers.requestTo("https://tr.wikipedia.org/api/rest_v1/page/summary/An%C4%B1tkabir"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"extract\":\"Mustafa Kemal Atatürk'ün kabri\"}", MediaType.APPLICATION_JSON));
                
        // Ensure system instruction is included in the request
        mockServer.expect(MockRestRequestMatchers.requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent"))
                .andExpect(MockRestRequestMatchers.jsonPath("$.systemInstruction.parts[0].text").exists())
                .andExpect(MockRestRequestMatchers.jsonPath("$.contents[0].parts[0].text").value(org.hamcrest.Matchers.containsString("BİLGİLER:")))
                .andRespond(MockRestResponseCreators.withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"*Evet*, çok _güzel_ bir #yerdir#.\"}]}}]}", MediaType.APPLICATION_JSON));

        SoruYaniti yanit = soruServisi.serbestSoru(1L, "Güzel mi?", 100L);
        
        assertNotNull(yanit);
        assertEquals("Evet, çok güzel bir yerdir.", yanit.cevap());
        assertFalse(yanit.onbellekten());
        mockServer.verify();
    }
    
    @Test
    void sunucuHatasi503_Istisna() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        SoruServisi soruServisi = createService(builder, "test-key");
        
        GeometryFactory gf = new GeometryFactory();
        Place p = mock(Place.class);
        when(p.getId()).thenReturn(1L);
        when(p.getAd()).thenReturn("Anıtkabir");
        when(p.getKategori()).thenReturn("museum");
        when(p.getTur()).thenReturn("gezi");
        when(p.getKonum()).thenReturn(gf.createPoint(new Coordinate(32.8, 39.9)));
        when(p.getCalismaSaatleri()).thenReturn("09:00-17:00");
        when(placeRepository.findById(1L)).thenReturn(Optional.of(p));

        mockServer.expect(MockRestRequestMatchers.requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent"))
                .andRespond(MockRestResponseCreators.withServerError());

        ApiIstisnasi ex = assertThrows(ApiIstisnasi.class, () -> {
            soruServisi.hazirSoru(1L, "deger");
        });
        assertEquals(503, ex.getStatus().value());
    }
}
