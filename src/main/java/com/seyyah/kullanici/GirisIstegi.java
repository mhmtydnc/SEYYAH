package com.seyyah.kullanici;

import jakarta.validation.constraints.NotBlank;

public record GirisIstegi(@NotBlank String eposta, @NotBlank String sifre) {
}
