-- 4_payment_service.sql
-- Creates `transactions` table for the Payment Service

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS transactions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL,
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  amount_cents BIGINT NOT NULL,
  status VARCHAR(50) NOT NULL,
  gateway_response TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transactions_order ON transactions(order_id);
