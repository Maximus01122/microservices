-- Add ticket_id and ticket_qr to order_items for issued tickets
-- Idempotent: safe to run multiple times
BEGIN;
ALTER TABLE IF EXISTS order_items
  ADD COLUMN IF NOT EXISTS ticket_id uuid,
  ADD COLUMN IF NOT EXISTS ticket_qr text;
COMMIT;
