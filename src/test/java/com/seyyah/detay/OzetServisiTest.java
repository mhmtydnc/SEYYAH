package com.seyyah.detay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OzetServisiTest {

    private JdbcClient jdbcClient;
    
    @BeforeEach
    void setup() {
        jdbcClient = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        when(jdbcClient.sql(anyString()).param(anyString()).query(Integer.class).single()).thenReturn(1);
    }

    private OzetServisi createService(RestClient.Builder builder, String key) {
        GeminiIstemcisi gemini = new GeminiIstemcisi(builder, key, "gemini-flash-lite-latest");
        VikipediMetni wiki = new VikipediMetni(builder);
        return new OzetServisi(gemini, wiki, jdbcClient);
    }

    @Test
    void basariliOzet_MarkdownTemizligi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        OzetServisi ozetServisi = createService(builder, "test-key");
        
        mockServer.expect(MockRestRequestMatchers.requestTo("https://tr.wikipedia.org/api/rest_v1/page/summary/Ankara"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"extract\":\"Ankara çok güzel bir şehirdir. Ve bayağı uzun bir metindir bu, en azından seksen karakteri geçmesi gerekiyor ki test başarılı olsun ve null dönmesin, aksi halde test patlar.\"}", MediaType.APPLICATION_JSON));
                
        mockServer.expect(MockRestRequestMatchers.requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"*Ankara* çok _güzel_ bir #şehirdir#.\"}]}}]}", MediaType.APPLICATION_JSON));

        OzetDetay detay = ozetServisi.ozetUret(new WikidataDetay("aciklama", "https://tr.wikipedia.org/wiki/Ankara", null));
        
        assertNotNull(detay);
        assertEquals("Ankara çok güzel bir şehirdir.", detay.metin());
        assertEquals("https://tr.wikipedia.org/wiki/Ankara", detay.vikipedi());
    }
    
    @Test
    void kisaExtract_Null() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        OzetServisi ozetServisi = createService(builder, "test-key");
        
        mockServer.expect(MockRestRequestMatchers.requestTo("https://tr.wikipedia.org/api/rest_v1/page/summary/Ankara"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"extract\":\"Çok kısa.\"}", MediaType.APPLICATION_JSON));

        OzetDetay detay = ozetServisi.ozetUret(new WikidataDetay("aciklama", "https://tr.wikipedia.org/wiki/Ankara", null));
        assertNull(detay);
    }
    
    @Test
    void sunucuHatasi503_Exception() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        OzetServisi ozetServisi = createService(builder, "test-key");
        
        mockServer.expect(MockRestRequestMatchers.requestTo("https://tr.wikipedia.org/api/rest_v1/page/summary/Ankara"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"extract\":\"Ankara çok güzel bir şehirdir. Ve bayağı uzun bir metindir bu, en azından seksen karakteri geçmesi gerekiyor ki test başarılı olsun ve null dönmesin, aksi halde test patlar.\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(MockRestRequestMatchers.requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent"))
                .andRespond(MockRestResponseCreators.withServerError());

        assertThrows(RuntimeException.class, () -> {
            ozetServisi.ozetUret(new WikidataDetay("aciklama", "https://tr.wikipedia.org/wiki/Ankara", null));
        });
    }

    @Test
    void anahtarYok_CagriYok() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        OzetServisi ozetServisi = createService(builder, "");
        OzetDetay detay = ozetServisi.ozetUret(new WikidataDetay("aciklama", "https://tr.wikipedia.org/wiki/Ankara", null));
        assertNull(detay);
        mockServer.verify(); // No requests should be made
    }
    
    @Test
    void turkceKarakterliBaslik_TekKodlanmis() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        OzetServisi ozetServisi = createService(builder, "test-key");
        
        mockServer.expect(MockRestRequestMatchers.requestTo("https://tr.wikipedia.org/api/rest_v1/page/summary/An%C4%B1tkabir"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withSuccess("{\"extract\":\"Anıtkabir, Mustafa Kemal Atatürk'ün anıt mezarıdır. Ve seksen karakteri geçmesi için biraz daha yazı ekliyorum.\"}", MediaType.APPLICATION_JSON));
                
        mockServer.expect(MockRestRequestMatchers.requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent"))
                .andRespond(MockRestResponseCreators.withSuccess("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Atatürk'ün anıt mezarıdır.\"}]}}]}", MediaType.APPLICATION_JSON));

        OzetDetay detay = ozetServisi.ozetUret(new WikidataDetay("aciklama", "https://tr.wikipedia.org/wiki/An%C4%B1tkabir", null));
        assertNotNull(detay);
    }
}
