CREATE TABLE yer_detay_onbellek (
    yer_id BIGINT PRIMARY KEY REFERENCES places(id) ON DELETE CASCADE,
    wikidata JSONB,
    wikidata_zamani TIMESTAMPTZ,
    google_place_id TEXT,
    google JSONB,
    google_zamani TIMESTAMPTZ
);

CREATE TABLE api_kullanim (
    ay CHAR(7),
    servis TEXT,
    sayi INT NOT NULL DEFAULT 0,
    PRIMARY KEY (ay, servis)
);
