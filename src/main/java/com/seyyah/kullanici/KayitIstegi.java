package com.seyyah.kullanici;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KayitIstegi(
        @NotBlank @Size(max = 60) String ad,
        @NotBlank @Email String eposta,
        @NotBlank @Size(min = 8, max = 72) String sifre) {
}
