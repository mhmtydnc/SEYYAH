package com.seyyah.rotalarim;

import com.seyyah.route.AraNokta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RotaKaydetIstegi(
        @NotBlank @Size(max = 200) String baslik,
        @Valid @NotNull Konum kalkis,
        @Valid @NotNull Konum varis,
        @Valid AraNokta uzerinden,
        @Valid @Size(max = 10) List<Durak> duraklar) {
}
