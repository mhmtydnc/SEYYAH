package com.seyyah.guvenlik;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// Üyelik ve kayıtlı rotalar için gereken tüm güvenlik bean'leri burada toplanır;
// @WebMvcTest dilimleri bu sınıfı import ederek aynı yapılandırmayı kullanabilir.
@Configuration
@EnableWebSecurity
public class GuvenlikAyari {

    @Bean
    public PasswordEncoder sifreKodlayici() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKeySpec jwtAnahtari(@Value("${jwt.gizli}") String gizliAnahtar) {
        return new SecretKeySpec(gizliAnahtar.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKeySpec jwtAnahtari) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtAnahtari));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKeySpec jwtAnahtari) {
        return NimbusJwtDecoder.withSecretKey(jwtAnahtari).macAlgorithm(MacAlgorithm.HS256).build();
    }

    // Token yok/geçersizse 401'i RFC 9457 ProblemDetail JSON olarak döndürür
    @Bean
    public AuthenticationEntryPoint girisNoktasi() {
        ObjectMapper mapper = new ObjectMapper();
        return (request, response, hata) -> {
            ProblemDetail govde = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED, "Giriş gerekli veya oturum belirteci geçersiz");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(mapper.writeValueAsString(govde));
        };
    }

    @Bean
    public SecurityFilterChain guvenlikZinciri(HttpSecurity http, JwtDecoder jwtDecoder,
                                                AuthenticationEntryPoint girisNoktasi) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(oturum -> oturum.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(temel -> temel.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(istek -> istek
                        .requestMatchers("/api/rotalarim/**", "/api/auth/ben").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(hata -> hata.authenticationEntryPoint(girisNoktasi))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .authenticationEntryPoint(girisNoktasi));

        return http.build();
    }
}
