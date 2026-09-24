package com.seyyah.detay;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yerler")
public class YerDetayController {

    private final YerDetayServisi yerDetayServisi;

    public YerDetayController(YerDetayServisi yerDetayServisi) {
        this.yerDetayServisi = yerDetayServisi;
    }

    @GetMapping("/{id}/detay")
    public YerDetay detayGetir(@PathVariable Long id) {
        return yerDetayServisi.detayGetir(id);
    }
}
