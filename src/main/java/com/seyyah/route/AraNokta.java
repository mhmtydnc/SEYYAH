package com.seyyah.route;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// "X üzerinden" rotanın ara şehri; kayıtlı rotada istemciden geldiği için sınırları doğrulanır
public record AraNokta(
        @NotBlank @Size(max = 120) String ad,
        @DecimalMin("-90") @DecimalMax("90") double enlem,
        @DecimalMin("-180") @DecimalMax("180") double boylam) {
}
