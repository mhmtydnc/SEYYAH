package com.seyyah.kullanici;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

// Belirteç 7 gün geçerli; subject kullanıcı id'si, ad/eposta claim olarak taşınır
@Service
public class JwtServisi {

    private static final long GECERLILIK_GUN = 7;

    private final JwtEncoder jwtEncoder;

    public JwtServisi(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String uret(Kullanici kullanici) {
        Instant simdi = Instant.now();
        JwtClaimsSet talepler = JwtClaimsSet.builder()
                .subject(String.valueOf(kullanici.getId()))
                .issuedAt(simdi)
                .expiresAt(simdi.plus(GECERLILIK_GUN, ChronoUnit.DAYS))
                .claim("ad", kullanici.getAd())
                .claim("eposta", kullanici.getEposta())
                .build();
        JwsHeader baslik = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(baslik, talepler)).getTokenValue();
    }
}
