package com.seyyah.detay;

import com.seyyah.kullanici.Kullanici;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SoruController {

    private final SoruServisi soruServisi;

    public SoruController(SoruServisi soruServisi) {
        this.soruServisi = soruServisi;
    }

    // Boş/eksik alan servisteki switch'e null gidip 500 veriyordu
    public record HazirSoruIstegi(@NotBlank String soru) {}

    @PostMapping("/yerler/{id}/hazir-soru")
    public SoruYaniti hazirSoru(@PathVariable Long id, @Valid @RequestBody HazirSoruIstegi istek) {
        return soruServisi.hazirSoru(id, istek.soru());
    }

    public record SerbestSoruIstegi(
            @NotBlank
            @Size(min = 3, max = 200)
            String metin
    ) {}

    @PostMapping("/uye/yerler/{id}/soru")
    public SoruYaniti serbestSoru(@PathVariable Long id,
                                  @Valid @RequestBody SerbestSoruIstegi istek,
                                  @AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        Long kullaniciId = Long.valueOf(jwt.getSubject());
        return soruServisi.serbestSoru(id, istek.metin(), kullaniciId);
    }
}
