package com.seyyah.konum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KonumController.class)
class KonumControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private KonumServisi konumServisi;

    @Test
    void aramaBasarili() throws Exception {
        given(konumServisi.ara("Kapadokya", 5))
                .willReturn(List.of(new KonumSonucu("Göreme", "Göreme, TR", 38.64, 34.83)));

        mvc.perform(get("/api/konum/ara?q=Kapadokya&limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ad").value("Göreme"))
                .andExpect(jsonPath("$[0].etiket").value("Göreme, TR"))
                .andExpect(jsonPath("$[0].enlem").value(38.64))
                .andExpect(jsonPath("$[0].boylam").value(34.83));
    }

    @Test
    void qCokKisa400Olur() throws Exception {
        mvc.perform(get("/api/konum/ara?q=K"))
                .andExpect(status().isBadRequest());
    }
}
