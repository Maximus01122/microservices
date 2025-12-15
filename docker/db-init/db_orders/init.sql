-- Init for db_orders (orders service)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Reusable function to keep updated_at current (defined per-DB for isolated deployments)
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS orders (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID,
  user_email VARCHAR(255),
  status VARCHAR(50) NOT NULL DEFAULT 'IN_CART',
  total_amount BIGINT NOT NULL DEFAULT 0,
  tax_amount BIGINT NOT NULL DEFAULT 0,
  currency VARCHAR(10) NOT NULL DEFAULT 'CAD',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TRIGGER orders_updated_at
  BEFORE UPDATE ON orders
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at();

CREATE TABLE IF NOT EXISTS order_items (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
  event_id UUID,
  seat_id VARCHAR(50),
  unit_price_cents BIGINT NOT NULL,
  ticket_id UUID,
  ticket_qr TEXT,
  reservation_id UUID
);

CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
