package com.seyyah.konum;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/konum")
@Validated
public class KonumController {

    private final KonumServisi konumServisi;

    public KonumController(KonumServisi konumServisi) {
        this.konumServisi = konumServisi;
    }

    @GetMapping("/ara")
    public List<KonumSonucu> ara(
            @RequestParam @Size(min = 2, message = "Arama metni en az 2 karakter olmalıdır") String q,
            @RequestParam(defaultValue = "5") @Min(1) @Max(10) int limit) {
        return konumServisi.ara(q, limit);
    }
}
