-- Init for db_payment (payment service)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Reusable function to keep updated_at current (defined per-DB for isolated deployments)
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS transactions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  order_id BIGINT,
  user_id UUID,
  correlation_id varchar(128),
  amount_cents BIGINT NOT NULL,
  status VARCHAR(50) NOT NULL,
  gateway_response TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transactions_order ON transactions(order_id);

CREATE TABLE IF NOT EXISTS payment_sessions (
  correlation_id varchar(128) PRIMARY KEY,
  order_id bigint,
  amount_cents bigint NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'PENDING',
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_payment_sessions_order ON payment_sessions(order_id);
