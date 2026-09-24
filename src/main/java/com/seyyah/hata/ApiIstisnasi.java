package com.seyyah.hata;

import org.springframework.http.HttpStatus;

// Üyelik ve kayıtlı rotalar gibi yeni modüllerin, istemciye hangi HTTP durumuyla
// yansıtılacağını taşıyan genel amaçlı hatası (bkz. route.RotaServisiException)
public class ApiIstisnasi extends RuntimeException {

    private final HttpStatus status;

    public ApiIstisnasi(HttpStatus status, String mesaj) {
        super(mesaj);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
