-- 2_event_ticket_service.sql
-- Creates `events` and `tickets` tables for the Event-Ticket Service

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS events (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name VARCHAR(255) NOT NULL,
  description TEXT,
  venue VARCHAR(1024),
  start_time TIMESTAMP WITH TIME ZONE,
  rows INTEGER,
  cols INTEGER,
  creator_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
  seats JSONB,
  reservation_expires JSONB,
  reservation_holder JSONB,
  reservation_ids JSONB,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TRIGGER events_updated_at
  BEFORE UPDATE ON events
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at();

CREATE TABLE IF NOT EXISTS tickets (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  event_id UUID REFERENCES events(id) ON DELETE CASCADE,
  seat_id VARCHAR(50),
  owner_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  issued_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  qr_code_url VARCHAR(2048)
);

CREATE INDEX IF NOT EXISTS idx_events_creator ON events(creator_user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_event ON tickets(event_id);
