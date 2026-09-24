package com.seyyah.dagitim;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * React Router yolları (/giris, /rotalarim, ...) sayfa yenilenince 404 olmasın diye: /api, /actuator
 * ve dosya uzantılı (nokta içeren) yollar dışındaki GET istekleri index.html'e forward edilir.
 * static/index.html henüz yoksa (geliştirme ortamı) forward normal 404'e düşer, hata fırlatmaz.
 */
@Component
class SpaYonlendirmeFiltresi extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest istek, HttpServletResponse yanit, FilterChain zincir)
            throws ServletException, IOException {
        if (spaYoluMu(istek)) {
            istek.getRequestDispatcher("/index.html").forward(istek, yanit);
            return;
        }
        zincir.doFilter(istek, yanit);
    }

    private boolean spaYoluMu(HttpServletRequest istek) {
        if (!HttpMethod.GET.matches(istek.getMethod())) {
            return false;
        }
        String yol = istek.getRequestURI();
        if (yol.startsWith("/api/") || yol.startsWith("/actuator/")) {
            return false;
        }
        String sonSegment = yol.substring(yol.lastIndexOf('/') + 1);
        return !sonSegment.contains(".");
    }
}
