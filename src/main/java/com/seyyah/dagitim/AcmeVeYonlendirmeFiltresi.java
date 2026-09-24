package com.seyyah.dagitim;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * 80 numaralı bağlayıcıya (win-acme yenilemesi + tarayıcıların http'ye düşmesi) gelen istekleri karşılar:
 * ACME doğrulama dosyalarını acme dizininden sunar, geri kalan her isteği aynı yolun https karşılığına
 * 301 ile yönlendirir. 443'teki bağlayıcıdan gelen istekler bu filtreden etkilenmeden normal akar.
 */
class AcmeVeYonlendirmeFiltresi extends OncePerRequestFilter {

    private static final String ACME_ON_EKI = "/.well-known/acme-challenge/";
    // Path traversal'a karşı: win-acme'nin ürettiği doğrulama dosya adları bu karakter kümesiyle sınırlı
    private static final Pattern GUVENLI_DOSYA_ADI = Pattern.compile("[A-Za-z0-9_-]+");

    private final int httpPortu;
    private final Path acmeDizini;

    AcmeVeYonlendirmeFiltresi(int httpPortu, String acmeDizini) {
        this.httpPortu = httpPortu;
        this.acmeDizini = Path.of(acmeDizini);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest istek, HttpServletResponse yanit, FilterChain zincir)
            throws ServletException, IOException {
        // Bu filtre her iki bağlayıcıya da bağlı; yalnızca 80'e gelen istekleri ele alır
        if (istek.getLocalPort() != httpPortu) {
            zincir.doFilter(istek, yanit);
            return;
        }

        String yol = istek.getRequestURI();
        if (yol.startsWith(ACME_ON_EKI)) {
            dogrulamaDosyasiSun(yol.substring(ACME_ON_EKI.length()), yanit);
            return;
        }

        String sorgu = istek.getQueryString();
        String hedef = "https://" + istek.getServerName() + yol + (sorgu == null ? "" : "?" + sorgu);
        yanit.setStatus(HttpStatus.MOVED_PERMANENTLY.value());
        yanit.setHeader("Location", hedef);
    }

    private void dogrulamaDosyasiSun(String dosyaAdi, HttpServletResponse yanit) throws IOException {
        if (!GUVENLI_DOSYA_ADI.matcher(dosyaAdi).matches()) {
            yanit.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }
        Path dosya = acmeDizini.resolve(dosyaAdi);
        if (!Files.isRegularFile(dosya)) {
            yanit.sendError(HttpStatus.NOT_FOUND.value());
            return;
        }
        yanit.setContentType(MediaType.TEXT_PLAIN_VALUE);
        Files.copy(dosya, yanit.getOutputStream());
    }
}
