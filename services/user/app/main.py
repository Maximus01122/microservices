import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.adapters.output.messaging import Broker
from app.adapters.input.http import router
from app import state
from app.config.database import engine, Base

@asynccontextmanager
# App lifecycle: connect AMQP on startup and close on shutdown.
async def lifespan(app: FastAPI):
    # Ensure schema has expected columns. If the table already exists, ALTER to add missing columns.
    from sqlalchemy import text
    import logging

    logger = logging.getLogger("user.migrations")

    alters = [
        # Ensure uuid extension and id column is UUID type so SQLAlchemy UUID PK comparisons work
        "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
        # Alter id column to uuid if it isn't already
        "ALTER TABLE IF EXISTS users ALTER COLUMN id TYPE UUID USING id::uuid;",
        "ALTER TABLE IF EXISTS users ALTER COLUMN id SET DEFAULT uuid_generate_v4();",
        "ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);",
        "ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT FALSE;",
        "ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS verification_token VARCHAR(255);",
        "ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS role VARCHAR(50) NOT NULL DEFAULT 'USER';",
        "ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT now();",
        "ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT now();",
    ]
    for stmt in alters:
        try:
            with engine.begin() as conn_tx:
                conn_tx.execute(text(stmt))
            logger.info("Executed migration: %s", stmt.strip())
        except Exception as e:
            logger.warning("Migration statement failed: %s -> %s", stmt.strip(), e)

    # Create tables (will not drop existing columns)
    Base.metadata.create_all(bind=engine)
    
    state.broker = Broker(os.getenv("AMQP_URL", "amqp://guest:guest@localhost:5672/"))
    await state.broker.connect()
    try:
        yield
    finally:
        if state.broker:
            await state.broker.close()


app = FastAPI(title="TicketChief - User Service", lifespan=lifespan)
app.include_router(router)


def get_port() -> int:
    try:
        return int(os.getenv("SERVICE_PORT", "3002"))
    except Exception:
        return 3002


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=get_port(), reload=False)
