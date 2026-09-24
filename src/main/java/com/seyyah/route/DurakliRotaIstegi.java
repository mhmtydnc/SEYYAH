package com.seyyah.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DurakliRotaIstegi(
        @Valid
        @NotNull(message = "Nokta listesi gerekli")
        @Size(min = 2, max = 13, message = "Nokta sayısı 2 ile 13 arasında olmalıdır")
        List<Nokta> noktalar
) {
    public record Nokta(
            @DecimalMin(value = "-90", message = "Enlem -90 ile 90 arasında olmalıdır")
            @DecimalMax(value = "90", message = "Enlem -90 ile 90 arasında olmalıdır")
            double enlem,

            @DecimalMin(value = "-180", message = "Boylam -180 ile 180 arasında olmalıdır")
            @DecimalMax(value = "180", message = "Boylam -180 ile 180 arasında olmalıdır")
            double boylam,

            Long durakId
    ) {
    }
}
