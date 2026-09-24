CREATE TABLE yer_soru_onbellek (
    yer_id BIGINT REFERENCES places(id) ON DELETE CASCADE,
    soru TEXT,
    cevap TEXT NOT NULL,
    olusturulma TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (yer_id, soru)
);
