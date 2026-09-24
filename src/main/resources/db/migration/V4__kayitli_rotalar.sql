CREATE TABLE kayitli_rotalar (
                                 id            BIGSERIAL PRIMARY KEY,
                                 kullanici_id  BIGINT           NOT NULL REFERENCES kullanicilar (id) ON DELETE CASCADE,
                                 baslik        TEXT             NOT NULL,
                                 kalkis_ad     TEXT             NOT NULL,
                                 kalkis_enlem  DOUBLE PRECISION NOT NULL,
                                 kalkis_boylam DOUBLE PRECISION NOT NULL,
                                 varis_ad      TEXT             NOT NULL,
                                 varis_enlem   DOUBLE PRECISION NOT NULL,
                                 varis_boylam  DOUBLE PRECISION NOT NULL,
                                 olusturulma   TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE INDEX idx_kayitli_rotalar_kullanici ON kayitli_rotalar (kullanici_id);
