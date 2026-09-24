package com.seyyah.kullanici;

import com.seyyah.guvenlik.GuvenlikAyari;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GuvenlikAyari import edilir (bkz. GuvenlikAyari yorumu); jwt.gizli sadece bağlamın açılması için gerekir
@WebMvcTest(AuthController.class)
@Import(GuvenlikAyari.class)
@TestPropertySource(properties = "jwt.gizli=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcd")
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private KullaniciRepository kullaniciRepository;

    @MockitoBean
    private JwtServisi jwtServisi;

    @Test
    void kayitBasariylaOlusturulurVe201Doner() throws Exception {
        when(kullaniciRepository.findByEposta("mehmet@ornek.com")).thenReturn(Optional.empty());
        when(kullaniciRepository.save(any(Kullanici.class))).thenAnswer(cagri -> {
            Kullanici kullanici = cagri.getArgument(0);
            kullanici.setId(1L);
            return kullanici;
        });
        when(jwtServisi.uret(any(Kullanici.class))).thenReturn("test-token");

        mvc.perform(post("/api/auth/kayit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ad\":\"Mehmet\",\"eposta\":\"Mehmet@Ornek.com\",\"sifre\":\"sifre1234\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.kullanici.id").value(1))
                .andExpect(jsonPath("$.kullanici.eposta").value("mehmet@ornek.com"));
    }

    @Test
    void ayniEpostaTekrarKayitOlursa409Doner() throws Exception {
        Kullanici mevcut = new Kullanici();
        mevcut.setId(2L);
        when(kullaniciRepository.findByEposta("mehmet@ornek.com")).thenReturn(Optional.of(mevcut));

        mvc.perform(post("/api/auth/kayit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ad\":\"Mehmet\",\"eposta\":\"mehmet@ornek.com\",\"sifre\":\"sifre1234\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void hataliGirisde401VeGenelMesajDoner() throws Exception {
        when(kullaniciRepository.findByEposta("olmayan@ornek.com")).thenReturn(Optional.empty());

        mvc.perform(post("/api/auth/giris")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eposta\":\"olmayan@ornek.com\",\"sifre\":\"yanlissifre\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-posta veya şifre hatalı"));
    }

    @Test
    void benUcuTokensiz401Doner() throws Exception {
        mvc.perform(get("/api/auth/ben"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void benUcuGecerliTokenIle200Doner() throws Exception {
        Kullanici kullanici = new Kullanici();
        kullanici.setId(1L);
        kullanici.setAd("Mehmet");
        kullanici.setEposta("mehmet@ornek.com");
        when(kullaniciRepository.findById(1L)).thenReturn(Optional.of(kullanici));

        mvc.perform(get("/api/auth/ben").with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ad").value("Mehmet"))
                .andExpect(jsonPath("$.eposta").value("mehmet@ornek.com"));
    }
}
