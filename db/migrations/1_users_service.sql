-- 1_users_service.sql
-- Creates the `users` table for the User Service
-- Adds uuid extension and a reusable trigger to update `updated_at` timestamps

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Reusable function to keep updated_at current
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  email VARCHAR(255) NOT NULL UNIQUE,
  name VARCHAR(255),
  password_hash VARCHAR(255),
  is_verified BOOLEAN NOT NULL DEFAULT FALSE,
  verification_token VARCHAR(255),
  role VARCHAR(50) NOT NULL DEFAULT 'USER',
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TRIGGER users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at();

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
