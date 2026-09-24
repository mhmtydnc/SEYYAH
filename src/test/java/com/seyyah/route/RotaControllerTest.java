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
    private PlaceRepository placeRepository;

    @Test
    void rotaBasarili() throws Exception {
        given(openRouteService.getRoute(29.0, 41.0, 32.0, 39.0))
                .willReturn(new RotaSonucu("wkt", List.of(List.of(29.0, 41.0), List.of(32.0, 39.0)), 1000.0, 500.0));

        given(placeRepository.koridorda("wkt", "gezi", 5000, 20)).willReturn(List.of());
        given(placeRepository.koridorda("wkt", "mola", 5000, 20)).willReturn(List.of());
        given(placeRepository.koridorda("wkt", "destek", 5000, 20)).willReturn(List.of());

        mvc.perform(get("/api/rota?kalkisEnlem=41.0&kalkisBoylam=29.0&varisEnlem=39.0&varisBoylam=32.0&yaricap=5000&limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mesafeM").value(1000.0))
                .andExpect(jsonPath("$.sureSn").value(500.0))
                .andExpect(jsonPath("$.geometri").isArray())
                .andExpect(jsonPath("$.yerler.gezi").isArray())
                .andExpect(jsonPath("$.yerler.mola").isArray())
                .andExpect(jsonPath("$.yerler.destek").isArray());

        verify(openRouteService).getRoute(29.0, 41.0, 32.0, 39.0);
        verify(placeRepository).koridorda("wkt", "gezi", 5000, 20);
        verify(placeRepository).koridorda("wkt", "mola", 5000, 20);
        verify(placeRepository).koridorda("wkt", "destek", 5000, 20);
    }

    @Test
    void gecersizParametreler400() throws Exception {
        mvc.perform(get("/api/rota?kalkisEnlem=100.0&kalkisBoylam=29.0&varisEnlem=39.0&varisBoylam=32.0"))
                .andExpect(status().isBadRequest());
    }
}
