package com.seyyah.route;

import com.seyyah.place.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteController.class)
class RouteControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OpenRouteService openRouteService;

    @MockitoBean
    private PlaceRepository placeRepository;

    @Test
    void varsayilanParametrelerleKoridoruSorgular() throws Exception {
        when(openRouteService.getRouteWkt(29.0, 41.0, 32.8, 39.9)).thenReturn("WKT");
        when(placeRepository.koridorda("WKT", "gezi", 5000, 20)).thenReturn(List.of());

        mvc.perform(get("/api/routes/routeStreet")
                        .param("startLon", "29.0").param("startLat", "41.0")
                        .param("endLon", "32.8").param("endLat", "39.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(placeRepository).koridorda("WKT", "gezi", 5000, 20);
    }

    @Test
    void verilenTurYaricapVeLimitKullanilir() throws Exception {
        when(openRouteService.getRouteWkt(29.0, 41.0, 32.8, 39.9)).thenReturn("WKT");

        mvc.perform(get("/api/routes/routeStreet")
                        .param("startLon", "29.0").param("startLat", "41.0")
                        .param("endLon", "32.8").param("endLat", "39.9")
                        .param("tur", "mola").param("yaricap", "2000").param("limit", "10"))
                .andExpect(status().isOk());

        verify(placeRepository).koridorda("WKT", "mola", 2000, 10);
    }

    @Test
    void gecersizEnlem400Doner() throws Exception {
        mvc.perform(get("/api/routes/routeStreet")
                        .param("startLon", "29.0").param("startLat", "95")
                        .param("endLon", "32.8").param("endLat", "39.9"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(openRouteService);
    }

    @Test
    void gecersizTur400Doner() throws Exception {
        mvc.perform(get("/api/routes/routeStreet")
                        .param("startLon", "29.0").param("startLat", "41.0")
                        .param("endLon", "32.8").param("endLat", "39.9")
                        .param("tur", "destek"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(openRouteService);
    }

    @Test
    void rotaBulunamazsaProblemDetailIle404Doner() throws Exception {
        when(openRouteService.getRouteWkt(29.0, 41.0, 32.8, 39.9))
                .thenThrow(new RotaServisiException(HttpStatus.NOT_FOUND, "Bu iki nokta arasında rota bulunamadı"));

        mvc.perform(get("/api/routes/routeStreet")
                        .param("startLon", "29.0").param("startLat", "41.0")
                        .param("endLon", "32.8").param("endLat", "39.9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Bu iki nokta arasında rota bulunamadı"));

        verifyNoInteractions(placeRepository);
    }
}
