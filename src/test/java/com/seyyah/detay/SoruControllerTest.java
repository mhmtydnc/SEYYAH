package com.seyyah.detay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seyyah.guvenlik.GuvenlikAyari;
import com.seyyah.hata.ApiIstisnasi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SoruController.class)
@Import(GuvenlikAyari.class)
@TestPropertySource(properties = "jwt.gizli=12345678901234567890123456789012")
public class SoruControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SoruServisi soruServisi;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void hazirSoru_gecersizAnahtar_400() throws Exception {
        when(soruServisi.hazirSoru(eq(1L), eq("gecersiz"))).thenThrow(new ApiIstisnasi(org.springframework.http.HttpStatus.BAD_REQUEST, "Geçersiz hazır soru"));
        
        mockMvc.perform(post("/api/yerler/1/hazir-soru")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"soru\":\"gecersiz\"}"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void serbestSoru_tokensiz_401() throws Exception {
        mockMvc.perform(post("/api/uye/yerler/1/soru")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metin\":\"Güzel mi?\"}"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void serbestSoru_jwtIle_200() throws Exception {
        when(soruServisi.serbestSoru(eq(1L), eq("Güzel mi?"), any())).thenReturn(new SoruYaniti("Evet", false));
        
        mockMvc.perform(post("/api/uye/yerler/1/soru")
                .with(jwt().jwt(j -> j.subject("100")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metin\":\"Güzel mi?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cevap").value("Evet"));
    }
    
    @Test
    void serbestSoru_201Karakter_400() throws Exception {
        String uzunMetin = "a".repeat(201);
        mockMvc.perform(post("/api/uye/yerler/1/soru")
                .with(jwt().jwt(j -> j.subject("100")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"metin\":\"" + uzunMetin + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hazirSoruAlaniEksikse400() throws Exception {
        mockMvc.perform(post("/api/yerler/1/hazir-soru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
