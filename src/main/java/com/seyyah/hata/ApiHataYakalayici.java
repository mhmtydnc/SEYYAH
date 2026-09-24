package com.seyyah.hata;

import com.seyyah.route.RotaServisiException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Hataları RFC 9457 ProblemDetail olarak döndürür. Üst sınıf parametre doğrulama
// (HandlerMethodValidationException) ve eksik parametre hatalarını 400'e çevirir.
@RestControllerAdvice
public class ApiHataYakalayici extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RotaServisiException.class)
    public ProblemDetail rotaServisi(RotaServisiException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(ApiIstisnasi.class)
    public ProblemDetail apiIstisnasi(ApiIstisnasi e) {
        return ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
    }
}
