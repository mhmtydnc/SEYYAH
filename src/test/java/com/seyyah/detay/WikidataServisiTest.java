package com.seyyah.detay;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(WikidataServisi.class)
public class WikidataServisiTest {

    @Autowired
    private WikidataServisi wikidataServisi;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void basariliTrVeP18() {
        String wRes = """
                {
                  "entities": {
                    "Q1": {
                      "descriptions": { "tr": { "value": "Test Aciklama" } },
                      "sitelinks": { "trwiki": { "title": "Test_Sayfa" } },
                      "claims": {
                        "P18": [
                          { "mainsnak": { "datavalue": { "value": "test.jpg" } } }
                        ]
                      }
                    }
                  }
                }
                """;

        String cRes = """
                {
                  "query": {
                    "pages": {
                      "123": {
                        "imageinfo": [
                          {
                            "thumburl": "http://thumb",
                            "descriptionurl": "http://desc",
                            "extmetadata": {
                              "Artist": { "value": "<a href=''>Ahmet</a>" },
                              "LicenseShortName": { "value": "CC-BY" }
                            }
                          }
                        ]
                      }
                    }
                  }
                }
                """;

        server.expect(requestTo("https://www.wikidata.org/w/api.php?action=wbgetentities&ids=Q1&props=descriptions%7Cclaims%7Csitelinks&languages=tr%7Cen&format=json"))
                .andRespond(withSuccess(wRes, MediaType.APPLICATION_JSON));
                
        server.expect(requestTo("https://commons.wikimedia.org/w/api.php?action=query&titles=File:test.jpg&prop=imageinfo&iiprop=url%7Cextmetadata&iiurlwidth=640&format=json"))
                .andRespond(withSuccess(cRes, MediaType.APPLICATION_JSON));

        WikidataDetay detay = wikidataServisi.detayGetir("Q1");
        
        assertThat(detay.aciklama()).isEqualTo("Test Aciklama");
        assertThat(detay.vikipedi()).isEqualTo("https://tr.wikipedia.org/wiki/Test_Sayfa");
        assertThat(detay.gorsel().url()).isEqualTo("http://thumb");
        assertThat(detay.gorsel().sayfa()).isEqualTo("http://desc");
        assertThat(detay.gorsel().yazar()).isEqualTo("Ahmet");
        assertThat(detay.gorsel().lisans()).isEqualTo("CC-BY");
    }
}
