package com.seyyah.route;

import com.seyyah.guvenlik.GuvenlikAyari;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.seyyah.place.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Import(GuvenlikAyari.class)
@TestPropertySource(properties = "jwt.gizli=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcd")
@WebMvcTest(RotaController.class)
class RotaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OpenRouteService openRouteService;

    @MockBean
    private AlternatifRotaServisi alternatifRotaServisi;

    @MockBean
    private PlaceRepository placeRepository;

    @Test
    void rotaBasarili() throws Exception {
        RotaSonucu ana = new RotaSonucu("wkt", List.of(List.of(29.0, 41.0), List.of(32.0, 39.0)), 1000.0, 500.0);
        given(openRouteService.getRoute(29.0, 41.0, 32.0, 39.0)).willReturn(ana);
        given(alternatifRotaServisi.alternatifleriBul(eq(ana), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of());

        given(placeRepository.koridorda("wkt", "gezi", 5000, 20)).willReturn(List.of());
        given(placeRepository.koridorda("wkt", "mola", 5000, 20)).willReturn(List.of());
        given(placeRepository.koridorda("wkt", "destek", 5000, 20)).willReturn(List.of());

        mvc.perform(get("/api/rota?kalkisEnlem=41.0&kalkisBoylam=29.0&varisEnlem=39.0&varisBoylam=32.0&yaricap=5000&limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rotalar").isArray())
                .andExpect(jsonPath("$.rotalar.length()").value(1))
                .andExpect(jsonPath("$.rotalar[0].sira").value(0))
                .andExpect(jsonPath("$.rotalar[0].ad").value("En hızlı"))
                .andExpect(jsonPath("$.rotalar[0].uzerinden").doesNotExist())
                .andExpect(jsonPath("$.rotalar[0].mesafeM").value(1000.0))
                .andExpect(jsonPath("$.rotalar[0].sureSn").value(500.0))
                .andExpect(jsonPath("$.rotalar[0].geometri").isArray())
                .andExpect(jsonPath("$.rotalar[0].yerler.gezi").isArray())
                .andExpect(jsonPath("$.rotalar[0].yerler.mola").isArray())
                .andExpect(jsonPath("$.rotalar[0].yerler.destek").isArray());

        verify(openRouteService).getRoute(29.0, 41.0, 32.0, 39.0);
        verify(placeRepository).koridorda("wkt", "gezi", 5000, 20);
        verify(placeRepository).koridorda("wkt", "mola", 5000, 20);
        verify(placeRepository).koridorda("wkt", "destek", 5000, 20);
    }

    @Test
    void alternatifRotalarSirayaEklenir() throws Exception {
        RotaSonucu ana = new RotaSonucu("ana-wkt", List.of(List.of(29.0, 41.0), List.of(32.0, 39.0)), 1000.0, 500.0);
        RotaSonucu alt = new RotaSonucu("alt-wkt", List.of(List.of(29.0, 41.0), List.of(32.0, 39.0)), 1200.0, 600.0);
        given(openRouteService.getRoute(29.0, 41.0, 32.0, 39.0)).willReturn(ana);
        given(alternatifRotaServisi.alternatifleriBul(eq(ana), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of(new AlternatifRotaServisi.RotaSecenegi(alt, "Aksaray üzerinden", "Aksaray")));

        given(placeRepository.koridorda(anyString(), anyString(), anyInt(), anyInt())).willReturn(List.of());

        mvc.perform(get("/api/rota?kalkisEnlem=41.0&kalkisBoylam=29.0&varisEnlem=39.0&varisBoylam=32.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rotalar.length()").value(2))
                .andExpect(jsonPath("$.rotalar[1].sira").value(1))
                .andExpect(jsonPath("$.rotalar[1].ad").value("Aksaray üzerinden"))
                .andExpect(jsonPath("$.rotalar[1].uzerinden").value("Aksaray"));
    }

    @Test
    void gecersizParametreler400() throws Exception {
        mvc.perform(get("/api/rota?kalkisEnlem=100.0&kalkisBoylam=29.0&varisEnlem=39.0&varisBoylam=32.0"))
                .andExpect(status().isBadRequest());
    }
}
