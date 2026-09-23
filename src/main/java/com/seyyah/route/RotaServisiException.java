package com.seyyah.route;

import org.springframework.http.HttpStatus;

// Rota servisinden (ORS) gelen hatanın istemciye hangi HTTP durumuyla yansıtılacağını taşır
public class RotaServisiException extends RuntimeException {

    private final HttpStatus status;

    public RotaServisiException(HttpStatus status, String mesaj) {
        super(mesaj);
        this.status = status;
    }

    public RotaServisiException(HttpStatus status, String mesaj, Throwable neden) {
        super(mesaj, neden);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
