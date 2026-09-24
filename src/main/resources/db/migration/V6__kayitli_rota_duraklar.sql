ALTER TABLE kayitli_rotalar
ADD COLUMN uzerinden JSONB NULL,
ADD COLUMN duraklar JSONB NOT NULL DEFAULT '[]'::jsonb;
