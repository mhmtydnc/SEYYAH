package com.seyyah.kullanici;

import com.seyyah.hata.ApiIstisnasi;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final KullaniciRepository kullaniciRepository;
    private final PasswordEncoder sifreKodlayici;
    private final JwtServisi jwtServisi;

    public AuthController(KullaniciRepository kullaniciRepository, PasswordEncoder sifreKodlayici,
                           JwtServisi jwtServisi) {
        this.kullaniciRepository = kullaniciRepository;
        this.sifreKodlayici = sifreKodlayici;
        this.jwtServisi = jwtServisi;
    }

    @PostMapping("/kayit")
    public ResponseEntity<KimlikYaniti> kayit(@Valid @RequestBody KayitIstegi istek) {
        String eposta = istek.eposta().toLowerCase();
        if (kullaniciRepository.findByEposta(eposta).isPresent()) {
            throw new ApiIstisnasi(HttpStatus.CONFLICT, "Bu e-posta adresi zaten kayıtlı");
        }

        Kullanici kullanici = new Kullanici();
        kullanici.setAd(istek.ad());
        kullanici.setEposta(eposta);
        kullanici.setSifreOzeti(sifreKodlayici.encode(istek.sifre()));
        kullanici = kullaniciRepository.save(kullanici);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new KimlikYaniti(jwtServisi.uret(kullanici), KullaniciOzet.olustur(kullanici)));
    }

    @PostMapping("/giris")
    public KimlikYaniti giris(@Valid @RequestBody GirisIstegi istek) {
        Kullanici kullanici = kullaniciRepository.findByEposta(istek.eposta().toLowerCase())
                .filter(k -> sifreKodlayici.matches(istek.sifre(), k.getSifreOzeti()))
                .orElseThrow(() -> new ApiIstisnasi(HttpStatus.UNAUTHORIZED, "E-posta veya şifre hatalı"));

        return new KimlikYaniti(jwtServisi.uret(kullanici), KullaniciOzet.olustur(kullanici));
    }

    @GetMapping("/ben")
    public KullaniciOzet ben(@AuthenticationPrincipal Jwt jwt) {
        Kullanici kullanici = kullaniciRepository.findById(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new ApiIstisnasi(HttpStatus.UNAUTHORIZED, "Kullanıcı bulunamadı"));

        return KullaniciOzet.olustur(kullanici);
    }
}
