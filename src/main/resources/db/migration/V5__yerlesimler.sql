CREATE OR REPLACE FUNCTION f_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE PARALLEL SAFE STRICT
AS $$
  SELECT public.unaccent('public.unaccent'::regdictionary, $1);
$$;

CREATE TABLE yerlesimler (
    id BIGSERIAL PRIMARY KEY,
    osm_type CHAR(1) NOT NULL,
    osm_id BIGINT NOT NULL,
    ad TEXT NOT NULL,
    tur TEXT NOT NULL,
    il TEXT,
    nufus INT,
    onem INT NOT NULL DEFAULT 0,
    konum geography(Point,4326) NOT NULL,
    UNIQUE(osm_type, osm_id)
);

CREATE INDEX yerlesimler_ad_trgm_idx ON yerlesimler USING gin (lower(f_unaccent(ad)) gin_trgm_ops);
CREATE INDEX places_ad_trgm_idx ON places USING gin (lower(f_unaccent(ad)) gin_trgm_ops);
CREATE INDEX yerlesimler_konum_gist_idx ON yerlesimler USING gist (konum);
