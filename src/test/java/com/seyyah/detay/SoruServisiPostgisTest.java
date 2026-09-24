package com.seyyah.detay;

import com.seyyah.PostgisTestDestegi;
import com.seyyah.hata.ApiIstisnasi;
import com.seyyah.place.Place;
import com.seyyah.place.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class SoruServisiPostgisTest extends PostgisTestDestegi {

    @Autowired
    private SoruServisi soruServisi;

    @Autowired
    private PlaceRepository placeRepository;
    
    @MockBean
    private GeminiIstemcisi geminiIstemcisi;

    @BeforeEach
    void setup() {
        placeRepository.deleteAll();
        GeometryFactory gf = new GeometryFactory();
        Place p = new Place();
        p.setOsmType("N");
        p.setOsmId(1L);
        p.setAd("Anıtkabir");
        p.setKategori("museum");
        p.setTur("gezi");
        p.setKonum(gf.createPoint(new Coordinate(32.8, 39.9)));
        p.setCalismaSaatleri("09:00-17:00");
        p.setOnemSkoru(1);
        p = placeRepository.save(p);
    }

    @Test
    void hazirSoru_ikinciIstekOnbellekten() {
        when(geminiIstemcisi.etkin()).thenReturn(true);
        when(geminiIstemcisi.icerikUret(any(), any())).thenReturn("Görülmeye değer.");
        
        SoruYaniti y1 = soruServisi.hazirSoru(1L, "deger");
        assertEquals("Görülmeye değer.", y1.cevap());
        assertFalse(y1.onbellekten());
        
        SoruYaniti y2 = soruServisi.hazirSoru(1L, "deger");
        assertEquals("Görülmeye değer.", y2.cevap());
        assertTrue(y2.onbellekten());
        
        verify(geminiIstemcisi, times(1)).icerikUret(any(), any());
    }
    
    @Test
    void serbestSoru_saatlikSinir11inciIstekte429() {
        when(geminiIstemcisi.etkin()).thenReturn(true);
        when(geminiIstemcisi.icerikUret(any(), any())).thenReturn("Evet.");
        
        for (int i = 0; i < 10; i++) {
            SoruYaniti y = soruServisi.serbestSoru(1L, "Güzel mi?", 500L);
            assertNotNull(y.cevap());
        }
        
        ApiIstisnasi ex = assertThrows(ApiIstisnasi.class, () -> {
            soruServisi.serbestSoru(1L, "Güzel mi?", 500L);
        });
        assertEquals(429, ex.getStatus().value());
    }
}
