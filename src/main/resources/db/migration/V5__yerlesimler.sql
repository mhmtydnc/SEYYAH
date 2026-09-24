-- unaccent STABLE olduğu için indeks ifadesinde kullanılamaz; sözlüğü sabitleyen sarmalayıcı IMMUTABLE olabilir
CREATE OR REPLACE FUNCTION f_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE PARALLEL SAFE STRICT
AS $$
  SELECT public.unaccent('public.unaccent'::regdictionary, $1);
$$;

CREATE TABLE yerlesimler (
    id       BIGSERIAL PRIMARY KEY,
    osm_type CHAR(1)   NOT NULL,
    osm_id   BIGINT    NOT NULL,
    ad       TEXT      NOT NULL,
    tur      TEXT      NOT NULL,
    il       TEXT,
    ilce     TEXT,
    nufus    INT,
    onem     INT       NOT NULL DEFAULT 0,
    konum    geography(Point, 4326) NOT NULL,
    CONSTRAINT uq_yerlesimler_osm UNIQUE (osm_type, osm_id)
);

CREATE INDEX idx_yerlesimler_konum ON yerlesimler USING GIST (konum);

-- Önek araması (2 harf de olsa) btree ile, yazım hatalı arama trigram ile indekslenir
CREATE INDEX idx_yerlesimler_ad_onek ON yerlesimler (lower(f_unaccent(ad)) text_pattern_ops);
CREATE INDEX idx_yerlesimler_ad_trgm ON yerlesimler USING GIN (lower(f_unaccent(ad)) gin_trgm_ops);
CREATE INDEX idx_places_ad_onek ON places (lower(f_unaccent(ad)) text_pattern_ops) WHERE tur = 'gezi';
CREATE INDEX idx_places_ad_norm_trgm ON places USING GIN (lower(f_unaccent(ad)) gin_trgm_ops) WHERE tur = 'gezi';
