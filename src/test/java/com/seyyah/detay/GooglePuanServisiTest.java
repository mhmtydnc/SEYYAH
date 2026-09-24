package com.seyyah.detay;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(GooglePuanServisi.class)
@TestPropertySource(properties = "google.places.anahtar=test-key")
public class GooglePuanServisiTest {

    @Autowired
    private GooglePuanServisi googlePuanServisi;

    @Autowired
    private MockRestServiceServer server;

    @MockitoBean(answers = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private JdbcClient jdbcClient;

    @Test
    void basariliCagri() {
        when(jdbcClient.sql(anyString()).param(anyString()).query(Integer.class).single()).thenReturn(1);

        String reqBody = """
                {"textQuery":"TestYer","languageCode":"tr","maxResultCount":1,"locationBias":{"circle":{"center":{"latitude":39.0,"longitude":35.0},"radius":500}}}
                """;

        String resBody = """
                {
                  "places": [
                    {
                      "id": "ChIJtest",
                      "rating": 4.5,
                      "userRatingCount": 100,
                      "googleMapsUri": "http://maps",
                      "location": { "latitude": 39.0, "longitude": 35.0 },
                      "reviews": [
                        {
                          "authorAttribution": { "displayName": "Ahmet", "uri": "http://ahmet" },
                          "rating": 5,
                          "text": { "text": "Çok güzel bir yer." },
                          "relativePublishTimeDescription": "2 ay önce"
                        }
                      ]
                    }
                  ]
                }
                """;

        server.expect(requestTo("https://places.googleapis.com/v1/places:searchText"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "test-key"))
                .andRespond(withSuccess(resBody, MediaType.APPLICATION_JSON));

        GoogleServisYaniti yanit = googlePuanServisi.detayGetir("TestYer", 39.0, 35.0, null);

        assertThat(yanit).isNotNull();
        assertThat(yanit.placeId()).isEqualTo("ChIJtest");
        assertThat(yanit.detay().puan()).isEqualTo(4.5);
        assertThat(yanit.detay().yorumlar()).hasSize(1);
        assertThat(yanit.detay().yorumlar().get(0).yazar()).isEqualTo("Ahmet");
    }
}
