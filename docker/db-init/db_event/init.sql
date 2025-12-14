-- Init for db_event (event-ticket service)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Reusable function to keep updated_at current (defined per-DB for isolated deployments)
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Note: if DBs are separated per container, cross-DB FKs aren't possible; event-ticket expects users to be external.

CREATE TABLE IF NOT EXISTS events (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name VARCHAR(255) NOT NULL,
  description TEXT,
  venue VARCHAR(1024),
  start_time TIMESTAMP WITH TIME ZONE,
  rows INTEGER,
  cols INTEGER,
  creator_user_id UUID,
  status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
  seats JSONB,
  reservation_expires JSONB,
  reservation_holder JSONB,
  reservation_ids JSONB,
  base_price_cents BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TRIGGER events_updated_at
  BEFORE UPDATE ON events
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at();

CREATE TABLE IF NOT EXISTS tickets (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  event_id UUID,
  seat_id VARCHAR(50),
  owner_user_id UUID,
  issued_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  qr_code_url VARCHAR(2048)
);

CREATE INDEX IF NOT EXISTS idx_events_creator ON events(creator_user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_event ON tickets(event_id);
