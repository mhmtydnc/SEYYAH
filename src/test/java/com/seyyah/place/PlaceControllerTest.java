package com.seyyah.place;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Spring 6.1+ yerleşik yöntem doğrulaması: @Validated olmadan da @Min/@Max/@Pattern çalışır
@WebMvcTest(PlaceController.class)
class PlaceControllerTest {

    private static final String WKT = "SRID=4326;LINESTRING(29 41, 32 39)";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PlaceRepository placeRepository;

    @Test
    void varsayilanlarlaSorgular() throws Exception {
        mvc.perform(get("/api/places/koridor").param("wkt", WKT))
                .andExpect(status().isOk());

        verify(placeRepository).koridorda(WKT, "gezi", 5000, 40);
    }

    @Test
    void yaricapSiniriAsilirsa400Doner() throws Exception {
        mvc.perform(get("/api/places/koridor").param("wkt", WKT).param("yaricap", "999999"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(placeRepository);
    }

    @Test
    void gecersizTur400Doner() throws Exception {
        mvc.perform(get("/api/places/koridor").param("wkt", WKT).param("tur", "hepsi"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(placeRepository);
    }

    @Test
    void wktEksikse400Doner() throws Exception {
        mvc.perform(get("/api/places/koridor"))
                .andExpect(status().isBadRequest());
    }
}
