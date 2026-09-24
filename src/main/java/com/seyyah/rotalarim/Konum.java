package com.seyyah.rotalarim;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Konum(
        @NotBlank String ad,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double enlem,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double boylam) {
}
