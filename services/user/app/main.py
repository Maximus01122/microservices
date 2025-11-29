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
    # Create tables
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
