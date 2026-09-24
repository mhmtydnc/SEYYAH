package com.seyyah.rotalarim;

import com.seyyah.guvenlik.GuvenlikAyari;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GuvenlikAyari import edilir (bkz. GuvenlikAyari yorumu); jwt.gizli sadece bağlamın açılması için gerekir
@WebMvcTest(KayitliRotaController.class)
@Import(GuvenlikAyari.class)
@TestPropertySource(properties = "jwt.gizli=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcd")
class KayitliRotaControllerTest {

    private static final String GOVDE = "{\"baslik\":\"İstanbul - Kapadokya\","
            + "\"kalkis\":{\"ad\":\"İstanbul\",\"enlem\":41.01,\"boylam\":28.97},"
            + "\"varis\":{\"ad\":\"Göreme\",\"enlem\":38.64,\"boylam\":34.83}}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private KayitliRotaRepository repo;

    @Test
    void tokensizIstek401Doner() throws Exception {
        mvc.perform(get("/api/rotalarim"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void kaydetVeListele() throws Exception {
        when(repo.save(any(KayitliRota.class))).thenAnswer(cagri -> {
            KayitliRota rota = cagri.getArgument(0);
            rota.setId(7L);
            return rota;
        });

        mvc.perform(post("/api/rotalarim")
                        .with(jwt().jwt(builder -> builder.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GOVDE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.baslik").value("İstanbul - Kapadokya"))
                .andExpect(jsonPath("$.kalkis.ad").value("İstanbul"));

        KayitliRota kayitli = new KayitliRota();
        kayitli.setId(7L);
        kayitli.setKullaniciId(1L);
        kayitli.setBaslik("İstanbul - Kapadokya");
        kayitli.setKalkisAd("İstanbul");
        kayitli.setKalkisEnlem(41.01);
        kayitli.setKalkisBoylam(28.97);
        kayitli.setVarisAd("Göreme");
        kayitli.setVarisEnlem(38.64);
        kayitli.setVarisBoylam(34.83);
        when(repo.findByKullaniciIdOrderByOlusturulmaDesc(1L)).thenReturn(List.of(kayitli));

        mvc.perform(get("/api/rotalarim").with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].varis.ad").value("Göreme"));
    }

    @Test
    void baskasininVeyaOlmayanRotayiSilmeyeCalisinca404Doner() throws Exception {
        when(repo.findByIdAndKullaniciId(5L, 1L)).thenReturn(Optional.empty());

        mvc.perform(delete("/api/rotalarim/5").with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void kaydetDurakli() throws Exception {
        when(repo.save(any(KayitliRota.class))).thenAnswer(cagri -> {
            KayitliRota rota = cagri.getArgument(0);
            rota.setId(8L);
            return rota;
        });

        String govdeDurakli = "{\"baslik\":\"İstanbul - Kapadokya\","
                + "\"kalkis\":{\"ad\":\"İstanbul\",\"enlem\":41.01,\"boylam\":28.97},"
                + "\"varis\":{\"ad\":\"Göreme\",\"enlem\":38.64,\"boylam\":34.83},"
                + "\"uzerinden\":{\"ad\":\"Aksaray\",\"enlem\":38.37,\"boylam\":34.03},"
                + "\"duraklar\":[{\"id\":118,\"ad\":\"Kırşehir Kalesi\",\"kategori\":\"castle\",\"enlem\":39.14,\"boylam\":34.16}]}";

        mvc.perform(post("/api/rotalarim")
                        .with(jwt().jwt(builder -> builder.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(govdeDurakli))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.uzerinden.ad").value("Aksaray"))
                .andExpect(jsonPath("$.duraklar[0].id").value(118));
    }

    @Test
    void durakAdiCokUzunsa400() throws Exception {
        String uzunAd = "a".repeat(201);
        String govde = "{\"baslik\":\"Deneme\","
                + "\"kalkis\":{\"ad\":\"A\",\"enlem\":41.0,\"boylam\":29.0},"
                + "\"varis\":{\"ad\":\"B\",\"enlem\":39.9,\"boylam\":32.8},"
                + "\"duraklar\":[{\"id\":1,\"ad\":\"" + uzunAd + "\",\"kategori\":\"castle\",\"enlem\":40.0,\"boylam\":30.0}]}";

        mvc.perform(post("/api/rotalarim")
                        .with(jwt().jwt(builder -> builder.subject("1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(govde))
                .andExpect(status().isBadRequest());
    }
}
