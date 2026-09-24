ALTER TABLE yer_detay_onbellek ADD COLUMN ozet JSONB;
ALTER TABLE yer_detay_onbellek ADD COLUMN ozet_zamani TIMESTAMPTZ;
ALTER TABLE api_kullanim ALTER COLUMN ay TYPE TEXT;
