package com.seyyah.dagitim;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

/**
 * Sadece prod profilinde ikinci bir Tomcat bağlayıcısı (80) açar: win-acme yenilemesi ve
 * http'den https'e yönlendirme için. 443'teki asıl bağlayıcı ve güvenlik zinciri etkilenmez.
 */
@Configuration
@Profile("prod")
public class HttpYonlendirmeYapilandirmasi {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpBaglayiciEkleyici(
            @Value("${seyyah.http-yonlendirme.port}") int httpPortu) {
        return factory -> {
            Connector baglayici = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            baglayici.setPort(httpPortu);
            baglayici.setScheme("http");
            baglayici.setSecure(false);
            factory.addAdditionalTomcatConnectors(baglayici);
        };
    }

    // Filtre elle oluşturulur (FilterRegistrationBean ile) ki hem 80 portu/acme dizini
    // yapılandırmadan gelsin hem de Spring Security zincirinden önce, en yüksek öncelikle çalışsın
    @Bean
    public FilterRegistrationBean<AcmeVeYonlendirmeFiltresi> acmeVeYonlendirmeFiltresi(
            @Value("${seyyah.http-yonlendirme.port}") int httpPortu,
            @Value("${seyyah.acme-dizini}") String acmeDizini) {
        FilterRegistrationBean<AcmeVeYonlendirmeFiltresi> kayit =
                new FilterRegistrationBean<>(new AcmeVeYonlendirmeFiltresi(httpPortu, acmeDizini));
        kayit.setOrder(Ordered.HIGHEST_PRECEDENCE);
        kayit.addUrlPatterns("/*");
        return kayit;
    }
}
