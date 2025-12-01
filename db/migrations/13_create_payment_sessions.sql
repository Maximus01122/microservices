-- Create payment_sessions to track in-flight payment requests
BEGIN;
CREATE TABLE IF NOT EXISTS payment_sessions (
  correlation_id varchar(128) PRIMARY KEY,
  order_id bigint,
  amount_cents bigint NOT NULL,
  status varchar(32) NOT NULL DEFAULT 'PENDING',
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_payment_sessions_order ON payment_sessions(order_id);
COMMIT;
