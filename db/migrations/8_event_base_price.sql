-- 8_event_base_price.sql
-- Adds base_price_cents to events so per-row seat pricing can be computed

BEGIN;
ALTER TABLE IF EXISTS events ADD COLUMN IF NOT EXISTS base_price_cents BIGINT NOT NULL DEFAULT 0;
COMMIT;