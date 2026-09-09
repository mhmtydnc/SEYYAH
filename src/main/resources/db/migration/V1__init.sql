CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE places (
                        id               BIGSERIAL PRIMARY KEY,
                        osm_type         CHAR(1)      NOT NULL,
                        osm_id           BIGINT       NOT NULL,
                        ad               TEXT         NOT NULL,
                        kategori         TEXT         NOT NULL,
                        konum            geography(Point, 4326) NOT NULL,
                        ucret            TEXT,
                        calisma_saatleri TEXT,
                        wikidata_id      TEXT,
                        website          TEXT,
                        onem_skoru       INT          NOT NULL DEFAULT 0,
                        created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
                        CONSTRAINT uq_places_osm UNIQUE (osm_type, osm_id)
);

CREATE INDEX idx_places_konum    ON places USING GIST (konum);
CREATE INDEX idx_places_kategori ON places (kategori);
CREATE INDEX idx_places_ad_trgm  ON places USING GIN (ad gin_trgm_ops);