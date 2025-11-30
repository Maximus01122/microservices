-- 5_notification_service.sql
-- Creates `email_logs` table for the Notification Service

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS email_logs (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  recipient VARCHAR(1024) NOT NULL,
  subject VARCHAR(1024),
  email_type VARCHAR(50),
  status VARCHAR(50) NOT NULL,
  error_message TEXT,
  retry_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  sent_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_email_logs_recipient ON email_logs(recipient);
