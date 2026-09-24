package com.seyyah.rotalarim;

import com.seyyah.hata.ApiIstisnasi;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rotalarim")
public class KayitliRotaController {

    private final KayitliRotaRepository repo;

    public KayitliRotaController(KayitliRotaRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RotaYaniti kaydet(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RotaKaydetIstegi istek) {
        KayitliRota rota = new KayitliRota();
        rota.setKullaniciId(kullaniciId(jwt));
        rota.setBaslik(istek.baslik());
        rota.setKalkisAd(istek.kalkis().ad());
        rota.setKalkisEnlem(istek.kalkis().enlem());
        rota.setKalkisBoylam(istek.kalkis().boylam());
        rota.setVarisAd(istek.varis().ad());
        rota.setVarisEnlem(istek.varis().enlem());
        rota.setVarisBoylam(istek.varis().boylam());

        return RotaYaniti.olustur(repo.save(rota));
    }

    @GetMapping
    public List<RotaYaniti> listele(@AuthenticationPrincipal Jwt jwt) {
        return repo.findByKullaniciIdOrderByOlusturulmaDesc(kullaniciId(jwt)).stream()
                .map(RotaYaniti::olustur)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sil(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        KayitliRota rota = repo.findByIdAndKullaniciId(id, kullaniciId(jwt))
                .orElseThrow(() -> new ApiIstisnasi(HttpStatus.NOT_FOUND, "Kayıtlı rota bulunamadı"));
        repo.delete(rota);
    }

    private Long kullaniciId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
