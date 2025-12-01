-- Add reservation_id to order_items so orders can reference event reservations
-- Idempotent: will add the column only if it doesn't exist
BEGIN;
ALTER TABLE IF EXISTS order_items
  ADD COLUMN IF NOT EXISTS reservation_id uuid;
COMMIT;
