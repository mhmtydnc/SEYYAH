package com.seyyah.detay;

import com.seyyah.guvenlik.GuvenlikAyari;
import com.seyyah.hata.ApiIstisnasi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(YerDetayController.class)
@Import(GuvenlikAyari.class)
@TestPropertySource(properties = "jwt.gizli=cokgizliuzunbiranahtaryeterliuzunlukta")
public class YerDetayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private YerDetayServisi yerDetayServisi;

    @Test
    void testBasariliDetay() throws Exception {
        YerDetay detay = new YerDetay(1L, "Test", "kategori", "gezi", 39.0, 35.0, null, null, null, null, null, null);
        when(yerDetayServisi.detayGetir(1L)).thenReturn(detay);

        mockMvc.perform(get("/api/yerler/1/detay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ad").value("Test"));
    }

    @Test
    void testBulunamadi() throws Exception {
        when(yerDetayServisi.detayGetir(99L)).thenThrow(new ApiIstisnasi(HttpStatus.NOT_FOUND, "Yer bulunamadı"));

        mockMvc.perform(get("/api/yerler/99/detay"))
                .andExpect(status().isNotFound());
    }
}
