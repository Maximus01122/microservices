import os
import uuid
from typing import Dict, Optional

import aio_pika
import orjson
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field, EmailStr
from contextlib import asynccontextmanager


# Request body to register a new user.
class UserCreate(BaseModel):
    email: EmailStr
    name: str = Field(..., min_length=1)


# User projection returned by API calls.
class UserView(BaseModel):
    id: str
    email: EmailStr
    name: str


# Mock login request (email only).
class LoginRequest(BaseModel):
    email: EmailStr


# AMQP adapter for publishing domain events to the exchange.
class Broker:
    def __init__(self, amqp_url: str, exchange_name: str = "ticketchief") -> None:
        self.amqp_url = amqp_url
        self.exchange_name = exchange_name
        self._connection: Optional[aio_pika.RobustConnection] = None
        self._channel: Optional[aio_pika.abc.AbstractChannel] = None
        self._exchange: Optional[aio_pika.abc.AbstractExchange] = None

    async def connect(self) -> None:
        self._connection = await aio_pika.connect_robust(self.amqp_url)
        self._channel = await self._connection.channel()
        self._exchange = await self._channel.declare_exchange(
            self.exchange_name, aio_pika.ExchangeType.TOPIC, durable=True
        )
        # Ensure a mirror/debug queue exists and receives all messages
        debug_queue = await self._channel.declare_queue("debug.events", durable=True)
        await debug_queue.bind(self._exchange, routing_key="#")

    async def close(self) -> None:
        if self._connection:
            await self._connection.close()

    async def publish(self, routing_key: str, message: dict) -> None:
        assert self._exchange is not None
        await self._exchange.publish(
            aio_pika.Message(body=orjson.dumps(message)), routing_key=routing_key
        )


users: Dict[str, UserView] = {}
broker: Optional[Broker] = None


@asynccontextmanager
# App lifecycle: connect AMQP on startup and close on shutdown.
async def lifespan(app: FastAPI):
    global broker
    broker = Broker(os.getenv("AMQP_URL", "amqp://guest:guest@localhost:5672/"))
    await broker.connect()
    try:
        yield
    finally:
        if broker:
            await broker.close()


app = FastAPI(title="TicketChief - User Service", lifespan=lifespan)


@app.get("/health")
async def health() -> dict:
    return {"status": "ok"}


@app.post("/users", response_model=UserView, status_code=201)
async def register_user(req: UserCreate) -> UserView:
    # Enforce unique email
    for u in users.values():
        if u.email == req.email:
            raise HTTPException(status_code=409, detail="email already exists")

    user_id = str(uuid.uuid4())
    view = UserView(id=user_id, email=req.email, name=req.name)
    users[user_id] = view

    assert broker is not None
    await broker.publish(
        "user.email.verification.requested",
        {"userId": user_id, "email": req.email},
    )

    return view


@app.get("/users/{user_id}", response_model=UserView)
# Get user by id
async def get_user(user_id: str) -> UserView:
    # Basic validation: user must exist
    user = users.get(user_id)
    if not user:
        raise HTTPException(status_code=404, detail="user not found")
    return user


@app.delete("/users/{user_id}", status_code=204)
# Delete user by id
async def delete_user(user_id: str) -> None:
    # Basic validation: user must exist
    if user_id in users:
        users.pop(user_id)
        return
    raise HTTPException(status_code=404, detail="user not found")

@app.post("/login")
# Mock login: find by email and return a dummy token
async def login(req: LoginRequest) -> dict:
    # Mock login: find by email and return a dummy token
    found = next((u for u in users.values() if u.email == req.email), None)
    if not found:
        raise HTTPException(status_code=401, detail="invalid credentials")
    token = f"dummy-{found.id}"
    return {"token": token, "userId": found.id}


def get_port() -> int:
    try:
        return int(os.getenv("SERVICE_PORT", "3002"))
    except Exception:
        return 3002


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=get_port(), reload=False)