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
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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
        byte[] anahtar = gizliAnahtar.getBytes(StandardCharsets.UTF_8);
        // HS256 en az 256 bit ister; kısa anahtar ilk girişte 500 yerine açılışta anlaşılır bir hata versin
        if (anahtar.length < 32) {
            throw new IllegalStateException("JWT_SECRET en az 32 bayt olmalı");
        }
        return new SecretKeySpec(anahtar, "HmacSHA256");
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
            // Belirtilmezse ISO-8859-1 yazılır ve Türkçe karakterler bozulur
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(mapper.writeValueAsString(govde));
        };
    }

    private static final String[] KORUMALI_YOLLAR = {"/api/rotalarim/**", "/api/auth/ben", "/api/uye/**"};

    // Token yalnızca korumalı yollarda okunur. Aksi hâlde süresi dolmuş token'ı tarayıcıda kalan kullanıcı,
    // herkese açık rota ve aramada bile 401 alıyordu (geçersiz Bearer, permitAll'dan önce reddediliyor).
    private static BearerTokenResolver korumaliYollardaTokenCozucu() {
        DefaultBearerTokenResolver varsayilan = new DefaultBearerTokenResolver();
        List<RequestMatcher> korumali = Arrays.stream(KORUMALI_YOLLAR)
                .map(yol -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(yol))
                .toList();
        return istek -> korumali.stream().anyMatch(m -> m.matches(istek)) ? varsayilan.resolve(istek) : null;
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
                        .requestMatchers(KORUMALI_YOLLAR).authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(hata -> hata.authenticationEntryPoint(girisNoktasi))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(korumaliYollardaTokenCozucu())
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .authenticationEntryPoint(girisNoktasi));

        return http.build();
    }
}
