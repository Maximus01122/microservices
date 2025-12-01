-- Add correlation_id to transactions so attempts are grouped per session
BEGIN;
ALTER TABLE IF EXISTS transactions ADD COLUMN IF NOT EXISTS correlation_id varchar(128);
CREATE INDEX IF NOT EXISTS idx_transactions_correlation ON transactions(correlation_id);
COMMIT;
