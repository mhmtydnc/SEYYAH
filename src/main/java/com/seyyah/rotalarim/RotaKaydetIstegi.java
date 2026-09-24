package com.seyyah.rotalarim;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RotaKaydetIstegi(
        @NotBlank @Size(max = 200) String baslik,
        @Valid @NotNull Konum kalkis,
        @Valid @NotNull Konum varis) {
}
