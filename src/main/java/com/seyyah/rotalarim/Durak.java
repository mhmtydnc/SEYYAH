package com.seyyah.rotalarim;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// JSONB'de saklanır; istemciden geldiği için metin uzunlukları sınırlı
public record Durak(
        Long id,
        @NotBlank @Size(max = 200) String ad,
        @Size(max = 60) String kategori,
        @DecimalMin("-90") @DecimalMax("90") double enlem,
        @DecimalMin("-180") @DecimalMax("180") double boylam) {
}
