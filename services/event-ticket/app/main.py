import asyncio
import base64
import os
import string
import uuid
from typing import Dict, List, Optional
import time

import orjson
import qrcode
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import aio_pika
from contextlib import asynccontextmanager


# --------------------
# DTOs (API contracts)
# --------------------


# Request body to create a new event with a grid seat map.
class EventCreate(BaseModel):
    name: str = Field(..., min_length=1)
    rows: int = Field(..., ge=1, le=26)  # up to Z
    cols: int = Field(..., ge=1, le=50)
    userId: str = Field(..., min_length=1)


# Request body to reserve seats for an event by seat IDs like 'A1'.
class ReserveRequest(BaseModel):
    userId: str = Field(..., min_length=1)
    seats: List[str] = Field(..., min_items=1)


# Read model of an event, including current seat statuses.
class EventView(BaseModel):
    id: str
    name: str
    rows: int
    cols: int
    seats: Dict[str, str]  # seatId -> status: available|reserved|confirmed


# Request body to verify a per-seat ticket for a specific event.
class TicketVerifyRequest(BaseModel):
    ticketId: str
    eventId: str


# Request body to rename an event (creator-only)
class EventRename(BaseModel):
    userId: str = Field(..., min_length=1)
    name: str = Field(..., min_length=1)


# Response indicating whether a ticket is valid for the given event.
class TicketVerifyResponse(BaseModel):
    valid: bool
    reason: Optional[str] = None
    ticketId: Optional[str] = None
    eventId: Optional[str] = None
    seat: Optional[str] = None


# --------------------
# In-memory domain state
# --------------------


# Event is a domain object that represents an event with its seats
class Event:
    def __init__(self, event_id: str, name: str, rows: int, cols: int) -> None:
        self.id = event_id
        self.name = name
        self.rows = rows
        self.cols = cols
        self.creator_user_id: Optional[str] = None
        self.seats: Dict[str, str] = {}
        self._initialize_seats()

    # Populate the seat grid as available (e.g., A1..A_N, B1..B_N).
    def _initialize_seats(self) -> None:
        letters = list(string.ascii_uppercase)[: self.rows]
        for r, letter in enumerate(letters, start=1):
            for c in range(1, self.cols + 1):
                self.seats[f"{letter}{c}"] = "available"


events: Dict[str, Event] = {}
# ticketId -> { "eventId": str, "seat": str }
tickets: Dict[str, Dict[str, object]] = {}

# Hold duration in seconds (default 10 minutes). Can be overridden by HOLD_MINUTES env.
hold_seconds: int = 600
_sweeper_task: Optional[asyncio.Task] = None


# Release a seat if its reservation has expired; return True if released
def _release_if_expired(event: Event, seat_id: str) -> bool:
    exp = getattr(event, "reservation_expires", {}).get(seat_id)
    if event.seats.get(seat_id) == "reserved" and exp is not None and exp <= time.time():
        event.seats[seat_id] = "available"
        event.reservation_expires.pop(seat_id, None)
        return True
    return False


# --------------------
# Messaging adapter
# --------------------


# AMQP adapter to publish domain events and consume commands.
class Broker:
    def __init__(self, amqp_url: str, exchange_name: str = "ticketchief") -> None:
        self.amqp_url = amqp_url
        self.exchange_name = exchange_name
        self._connection: Optional[aio_pika.RobustConnection] = None
        self._channel: Optional[aio_pika.abc.AbstractChannel] = None
        self._exchange: Optional[aio_pika.abc.AbstractExchange] = None
        self._consumer_task: Optional[asyncio.Task] = None

    # Open connection/channel and declare the topic exchange.
    async def connect(self) -> None:
        self._connection = await aio_pika.connect_robust(self.amqp_url)
        self._channel = await self._connection.channel()
        self._exchange = await self._channel.declare_exchange(
            self.exchange_name, aio_pika.ExchangeType.TOPIC, durable=True
        )

        # Debug queue
        debug_queue = await self._channel.declare_queue("debug.events", durable=True)
        await debug_queue.bind(self._exchange, routing_key="#")

    # Close consumer task (if any) and connection.
    async def close(self) -> None:
        if self._consumer_task and not self._consumer_task.done():
            self._consumer_task.cancel()
        if self._connection:
            await self._connection.close()

    # Publish a JSON message using the given routing key.
    async def publish(self, routing_key: str, message: dict) -> None:
        assert self._exchange is not None
        body = orjson.dumps(message)
        await self._exchange.publish(
            aio_pika.Message(body=body, content_type="application/json"),
            routing_key=routing_key,
        )

    # Bind and consume the payment.validated command with the provided handler.
    async def consume_payment_validated(self, handler) -> None:
        assert self._channel is not None
        assert self._exchange is not None
        queue = await self._channel.declare_queue("event-ticket.payment", durable=True)
        await queue.bind(self._exchange, routing_key="payment.validated")

        async def _on_message(message: aio_pika.abc.AbstractIncomingMessage) -> None:
            async with message.process():
                try:
                    payload = orjson.loads(message.body)
                    await handler(payload)
                except Exception as exc:  # meaningful: do not ack on crash
                    print(f"[event-ticket] handler error: {exc}")
                    raise

        await queue.consume(_on_message, no_ack=False)

        # Prevent method exit to keep consumer alive
        self._consumer_task = asyncio.create_task(asyncio.Event().wait())


broker: Optional[Broker] = None


# --------------------
# FastAPI setup
# --------------------


# lifespan is a context manager that starts the broker and handles the lifespan of the app
@asynccontextmanager
# App lifecycle: connect broker, start consumer, and cleanly shut down.
async def lifespan(app: FastAPI):
    global broker
    global hold_seconds, _sweeper_task
    amqp_url = os.getenv("AMQP_URL", "amqp://guest:guest@localhost:5672/")
    broker = Broker(amqp_url)
    await broker.connect()

    # Configure hold duration from environment (minutes)
    try:
        hold_minutes_env = int(os.getenv("HOLD_MINUTES", "10"))
        hold_seconds = max(60, hold_minutes_env * 60)
    except Exception:
        hold_seconds = 600

    async def handle_payment_validated(payload: dict) -> None:
        # Expected payload: { orderId, eventId, seats: [..], userId }
        # Confirm seats, then emit one ticket.created per seat (with PNG QR)
        event_id = payload.get("eventId")
        seat_ids: List[str] = payload.get("seats", [])
        payer_user_id: Optional[str] = payload.get("userId")
        event = events.get(event_id)
        if not event:
            print(f"payment for unknown event {event_id}")
            return
        # Confirm seats that were reserved
        holder_map: Dict[str, str] = getattr(event, "reservation_holder", {})
        confirmed_seats: List[str] = []
        for seat_id in seat_ids:
            # Must be reserved and belong to the paying user
            if event.seats.get(seat_id) == "reserved" and holder_map.get(seat_id) == payer_user_id:
                event.seats[seat_id] = "confirmed"
                confirmed_seats.append(seat_id)
                # Clear holder and expiry for confirmed seat
                getattr(event, "reservation_expires", {}).pop(seat_id, None)
                getattr(event, "reservation_holder", {}).pop(seat_id, None)

        # Create one ticket per confirmed seat, each with its own QR
        assert broker is not None
        if confirmed_seats:
            from io import BytesIO
            for seat_id in confirmed_seats:
                ticket_id = str(uuid.uuid4())
                qr_payload = {
                    "ticketId": ticket_id,
                    "eventId": event_id,
                    "seat": seat_id,
                }
                qr_img = qrcode.make(orjson.dumps(qr_payload).decode())
                buf = BytesIO()
                qr_img.save(buf, format="PNG")
                data_url = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()
                # Store issued ticket for later verification
                tickets[ticket_id] = {"eventId": event_id, "seat": seat_id}
                await broker.publish(
                    "ticket.created",
                    {
                        "ticketId": ticket_id,
                        "eventId": event_id,
                        "seat": seat_id,
                        "qr": data_url,
                    },
                )

            await broker.publish(
                "seat.confirmed",
                {"eventId": event_id, "seats": confirmed_seats},
            )

    # Start consumer in background
    asyncio.create_task(broker.consume_payment_validated(handle_payment_validated))

    # Start background sweeper to release expired reservations
    async def sweeper():
        while True:
            try:
                for ev in list(events.values()):
                    # Initialize mapping if missing
                    if not hasattr(ev, "reservation_expires"):
                        setattr(ev, "reservation_expires", {})
                    exp_map: Dict[str, float] = getattr(ev, "reservation_expires")
                    for seat_id in list(exp_map.keys()):
                        _release_if_expired(ev, seat_id)
                await asyncio.sleep(5)
            except asyncio.CancelledError:
                break
            except Exception:
                await asyncio.sleep(5)

    _sweeper_task = asyncio.create_task(sweeper())
    try:
        yield
    finally:
        if broker:
            await broker.close()
        if _sweeper_task and not _sweeper_task.done():
            _sweeper_task.cancel()


app = FastAPI(title="TicketChief - Event & Ticket Service", lifespan=lifespan)


# HTTP endpoints (application API)

# health is an endpoint to check if the service is running
@app.get("/health")
async def health() -> dict:
    return {"status": "ok"}


# POST /events to create a new event seat grid
@app.post("/events", response_model=EventView, status_code=201)
async def create_event(req: EventCreate) -> EventView:
    event_id = str(uuid.uuid4())
    event = Event(event_id, req.name, req.rows, req.cols)
    event.creator_user_id = req.userId
    events[event_id] = event
    return EventView(
        id=event.id,
        name=event.name,
        rows=event.rows,
        cols=event.cols,
        seats=event.seats,
    )


@app.get("/events/{event_id}", response_model=EventView)
 # Fetch current seat map with statuses for a given event.
async def get_event(event_id: str) -> EventView:
    event = events.get(event_id)
    if not event:
        raise HTTPException(status_code=404, detail="event not found")
    return EventView(
        id=event.id,
        name=event.name,
        rows=event.rows,
        cols=event.cols,
        seats=event.seats,
    )
@app.patch("/events/{event_id}", response_model=EventView)
 # Rename event title; only the creator can do this
async def rename_event(event_id: str, req: EventRename) -> EventView:
    event = events.get(event_id)
    if not event:
        raise HTTPException(status_code=404, detail="event not found")
    if event.creator_user_id != req.userId:
        raise HTTPException(status_code=403, detail="forbidden")
    event.name = req.name
    return EventView(
        id=event.id,
        name=event.name,
        rows=event.rows,
        cols=event.cols,
        seats=event.seats,
    )



@app.post("/events/{event_id}/reserve", status_code=200)
 # Reserve seats with business rules: same row, contiguous, ≤ 8 seats.
async def reserve_seats(event_id: str, req: ReserveRequest) -> dict:
    event = events.get(event_id)
    if not event:
        raise HTTPException(status_code=404, detail="event not found")

    # Business rules: max 8 seats; same row; contiguous columns
    if len(req.seats) > 8:
        raise HTTPException(status_code=400, detail="maximum 8 seats per reservation")

    def parse_seat(seat_id: str) -> tuple[str, int]:
        seat_id = seat_id.strip()
        if len(seat_id) < 2 or not seat_id[0].isalpha() or not seat_id[1:].isdigit():
            raise HTTPException(status_code=400, detail=f"invalid seat format {seat_id}")
        return seat_id[0].upper(), int(seat_id[1:])

    parsed = [parse_seat(s) for s in req.seats]
    rows = {r for r, _ in parsed}
    if len(rows) != 1:
        raise HTTPException(status_code=400, detail="seats must be in the same row")
    cols = sorted(c for _, c in parsed)
    if len(set(cols)) != len(cols):
        raise HTTPException(status_code=400, detail="duplicate seats in request")
    if any(b != a + 1 for a, b in zip(cols, cols[1:])):
        raise HTTPException(status_code=400, detail="seats must be contiguous")

    normalized_seats = [f"{r}{c}" for r, c in parsed]

    # Validate all seats exist and are available
    for seat_id in normalized_seats:
        if seat_id not in event.seats:
            raise HTTPException(status_code=400, detail=f"unknown seat {seat_id}")
        # Auto-release if reservation expired
        _release_if_expired(event, seat_id)
        # If seat is reserved by someone else, block
        holder_map: Dict[str, str] = getattr(event, "reservation_holder", {})
        if event.seats[seat_id] == "reserved":
            if holder_map.get(seat_id) != req.userId:
                raise HTTPException(status_code=409, detail=f"seat {seat_id} not available")
        elif event.seats[seat_id] != "available":
            raise HTTPException(status_code=409, detail=f"seat {seat_id} not available")

    # Reserve
    for seat_id in normalized_seats:
        event.seats[seat_id] = "reserved"
        # Track reservation expiry per seat
        if not hasattr(event, "reservation_expires"):
            setattr(event, "reservation_expires", {})
        getattr(event, "reservation_expires")[seat_id] = time.time() + hold_seconds
        # Track reservation holder per seat
        if not hasattr(event, "reservation_holder"):
            setattr(event, "reservation_holder", {})
        getattr(event, "reservation_holder")[seat_id] = req.userId

    # Publish domain event
    assert broker is not None
    await broker.publish(
        "seat.reserved",
        {"eventId": event_id, "seats": normalized_seats},
    )

    return {"eventId": event_id, "reserved": normalized_seats}


@app.post("/tickets/verify", response_model=TicketVerifyResponse)
 # Validate a per-seat ticket for a specific event and seat confirmed state.
async def verify_ticket(req: TicketVerifyRequest) -> TicketVerifyResponse:
    # Basic validation: ticket must exist and belong to the given event
    record = tickets.get(req.ticketId)
    if record is None:
        return TicketVerifyResponse(valid=False, reason="unknown ticket")
    if record.get("eventId") != req.eventId:
        return TicketVerifyResponse(valid=False, reason="ticket not for this event")
    # Optional: ensure seats are confirmed in the event state
    event = events.get(req.eventId)
    if not event:
        return TicketVerifyResponse(valid=False, reason="event not found")
    seat_id = str(record.get("seat"))
    if event.seats.get(seat_id) != "confirmed":
        return TicketVerifyResponse(valid=False, reason="seat not confirmed")
    return TicketVerifyResponse(valid=True, ticketId=req.ticketId, eventId=req.eventId, seat=seat_id)

 # Read listen port from env with a safe default.
def get_port() -> int:
    try:
        return int(os.getenv("SERVICE_PORT", "3001"))
    except Exception:
        return 3001


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=get_port(), reload=False)