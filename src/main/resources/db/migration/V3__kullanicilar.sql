CREATE TABLE kullanicilar (
                              id          BIGSERIAL PRIMARY KEY,
                              ad          TEXT        NOT NULL,
                              eposta      TEXT        NOT NULL,
                              sifre_ozeti TEXT        NOT NULL,
                              olusturulma TIMESTAMPTZ NOT NULL DEFAULT now(),
                              CONSTRAINT uq_kullanicilar_eposta UNIQUE (eposta)
);
