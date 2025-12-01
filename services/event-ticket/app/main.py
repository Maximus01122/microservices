import asyncio
import os
from contextlib import asynccontextmanager
from typing import Dict

from fastapi import FastAPI

from app import state
from app.adapters.input.consumers import handle_payment_validated
from app.adapters.input.http import router
from app.adapters.output.messaging import Broker
from app.domain.model import release_if_expired
from app.config.database import engine, Base, SessionLocal
from app.domain.entity import Event as EventEntity

@asynccontextmanager
# App lifecycle: connect broker, start consumer, and cleanly shut down.
async def lifespan(app: FastAPI):
    # Ensure schema has expected columns. If the table already exists, ALTER to add missing columns.
    import logging
    from sqlalchemy import text

    logger = logging.getLogger("event-ticket.migrations")
    # Execute each ALTER inside its own transaction so DDL is committed immediately and failures are visible
    alters = [
        # Ensure uuid extension and id columns are UUID typed so SQLAlchemy UUID PK comparisons work
        "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
        # Convert existing id columns to UUID type if necessary
        "ALTER TABLE IF EXISTS events ALTER COLUMN id TYPE UUID USING id::uuid;",
        "ALTER TABLE IF EXISTS events ALTER COLUMN id SET DEFAULT uuid_generate_v4();",
        "ALTER TABLE IF EXISTS tickets ALTER COLUMN id TYPE UUID USING id::uuid;",
        "ALTER TABLE IF EXISTS tickets ALTER COLUMN id SET DEFAULT uuid_generate_v4();",
        # Ensure foreign key columns are UUID as well
        "ALTER TABLE IF EXISTS tickets ALTER COLUMN event_id TYPE UUID USING event_id::uuid;",
        "ALTER TABLE IF EXISTS events ALTER COLUMN creator_user_id TYPE UUID USING creator_user_id::uuid;",
        "ALTER TABLE IF EXISTS events ADD COLUMN IF NOT EXISTS description TEXT;",
        "ALTER TABLE IF EXISTS events ADD COLUMN IF NOT EXISTS venue VARCHAR(1024);",
        "ALTER TABLE IF EXISTS events ADD COLUMN IF NOT EXISTS start_time TIMESTAMP WITH TIME ZONE;",
        "ALTER TABLE IF EXISTS events ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'DRAFT';",
        "ALTER TABLE IF EXISTS events ADD COLUMN IF NOT EXISTS seats JSONB;",
        "ALTER TABLE IF EXISTS events ADD COLUMN IF NOT EXISTS reservation_expires JSONB;",
        "ALTER TABLE IF EXISTS events ADD COLUMN IF NOT EXISTS reservation_holder JSONB;",
        "ALTER TABLE IF EXISTS events ADD COLUMN IF NOT EXISTS reservation_ids JSONB;",
        "ALTER TABLE IF EXISTS events ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT now();",
        "ALTER TABLE IF EXISTS events ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT now();",
    ]
    for stmt in alters:
        try:
            with engine.begin() as conn_tx:
                conn_tx.execute(text(stmt))
            logger.info("Successfully executed: %s", stmt.strip())
        except Exception as e:
            logger.warning("Failed to run statement '%s': %s", stmt.strip(), e)

    # Create tables (will not drop existing columns)
    Base.metadata.create_all(bind=engine)

    amqp_url = os.getenv("AMQP_URL", "amqp://guest:guest@localhost:5672/")
    state.broker = Broker(amqp_url)
    await state.broker.connect()

    # Configure hold duration from environment (minutes)
    try:
        hold_minutes_env = int(os.getenv("HOLD_MINUTES", "10"))
        state.hold_seconds = max(60, hold_minutes_env * 60)
    except Exception:
        state.hold_seconds = 600

    # Start consumer in background
    asyncio.create_task(state.broker.consume_payment_validated(handle_payment_validated))

    # Start background sweeper to release expired reservations
    async def sweeper():
        while True:
            try:
                db = SessionLocal()
                try:
                    events = db.query(EventEntity).all()
                    for event_entity in events:
                        # We need to map entity back to domain object logic or implement logic here
                        # For simplicity, reproducing logic inline with dict manipulation
                        current_seats = dict(event_entity.seats)
                        expires = dict(event_entity.reservation_expires or {})
                        
                        changed = False
                        import time
                        now = time.time()
                        
                        from datetime import datetime, timezone

                        def _expiry_to_ts(val):
                            # Accept numeric epoch (old format) or ISO8601 string (new format)
                            try:
                                return float(val)
                            except Exception:
                                try:
                                    if isinstance(val, str):
                                        v = val
                                        if v.endswith('Z'):
                                            v = v[:-1] + '+00:00'
                                        return datetime.fromisoformat(v).timestamp()
                                except Exception:
                                    return 0
                            return 0

                        for seat_id in list(expires.keys()):
                            exp_val = expires[seat_id]
                            exp_ts = _expiry_to_ts(exp_val)
                            if current_seats.get(seat_id) == "reserved" and exp_ts <= now:
                                current_seats[seat_id] = "available"
                                expires.pop(seat_id, None)
                                changed = True
                        
                        if changed:
                            event_entity.seats = current_seats
                            event_entity.reservation_expires = expires
                            db.commit()
                finally:
                    db.close()
                    
                await asyncio.sleep(5)
            except asyncio.CancelledError:
                break
            except Exception as e:
                print(f"Sweeper error: {e}")
                await asyncio.sleep(5)

    state.sweeper_task = asyncio.create_task(sweeper())
    try:
        yield
    finally:
        if state.broker:
            await state.broker.close()
        if state.sweeper_task and not state.sweeper_task.done():
            state.sweeper_task.cancel()


app = FastAPI(title="TicketChief - Event & Ticket Service", lifespan=lifespan)
app.include_router(router)


# Read listen port from env with a safe default.
def get_port() -> int:
    try:
        return int(os.getenv("SERVICE_PORT", "3001"))
    except Exception:
        return 3001


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=get_port(), reload=False)
