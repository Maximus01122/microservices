import time
import uuid
from typing import Dict, List

from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session

from app import state
from app.domain.model import (
    EventCreate,
    EventRename,
    EventView,
    ReserveRequest,
    TicketVerifyRequest,
    TicketVerifyResponse,
)
from app.domain.entity import Event as EventEntity, Ticket as TicketEntity
from app.config.database import get_db

router = APIRouter()

@router.get("/health")
async def health() -> dict:
    return {"status": "ok"}


@router.post("/events", response_model=EventView, status_code=201)
async def create_event(req: EventCreate, db: Session = Depends(get_db)) -> EventView:
    event_id = str(uuid.uuid4())
    
    # Initialize seats
    import string
    seats = {}
    letters = list(string.ascii_uppercase)[: req.rows]
    for r, letter in enumerate(letters, start=1):
        for c in range(1, req.cols + 1):
            seats[f"{letter}{c}"] = "available"

    event = EventEntity(
        id=event_id, 
        name=req.name, 
        rows=req.rows, 
        cols=req.cols, 
        creator_user_id=req.userId,
        seats=seats,
        reservation_expires={},
        reservation_holder={}
    )
    db.add(event)
    db.commit()
    db.refresh(event)
    
    return EventView(
        id=str(event.id),
        name=event.name,
        rows=event.rows,
        cols=event.cols,
        description=event.description,
        venue=event.venue,
        start_time=event.start_time,
        creator_user_id=str(event.creator_user_id) if event.creator_user_id else None,
        status=event.status or 'DRAFT',
        seats=event.seats,
        created_at=event.created_at,
        updated_at=event.updated_at,
    )


@router.get("/events/{event_id}", response_model=EventView)
 # Fetch current seat map with statuses for a given event.
async def get_event(event_id: str, db: Session = Depends(get_db)) -> EventView:
    event = db.query(EventEntity).filter(EventEntity.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="event not found")
    return EventView(
        id=str(event.id),
        name=event.name,
        rows=event.rows,
        cols=event.cols,
        description=event.description,
        venue=event.venue,
        start_time=event.start_time,
        creator_user_id=str(event.creator_user_id) if event.creator_user_id else None,
        status=event.status or 'DRAFT',
        seats=event.seats,
        created_at=event.created_at,
        updated_at=event.updated_at,
    )

@router.patch("/events/{event_id}", response_model=EventView)
 # Rename event title; only the creator can do this
async def rename_event(event_id: str, req: EventRename, db: Session = Depends(get_db)) -> EventView:
    event = db.query(EventEntity).filter(EventEntity.id == event_id).first()
    if not event:
        raise HTTPException(status_code=404, detail="event not found")
    if event.creator_user_id != req.userId:
        raise HTTPException(status_code=403, detail="forbidden")
    
    event.name = req.name
    db.commit()
    db.refresh(event)
    
    return EventView(
        id=str(event.id),
        name=event.name,
        rows=event.rows,
        cols=event.cols,
        description=event.description,
        venue=event.venue,
        start_time=event.start_time,
        creator_user_id=str(event.creator_user_id) if event.creator_user_id else None,
        status=event.status or 'DRAFT',
        seats=event.seats,
        created_at=event.created_at,
        updated_at=event.updated_at,
    )


@router.post("/events/{event_id}/reserve", status_code=200)
 # Reserve seats with business rules: same row, contiguous, ≤ 8 seats.
async def reserve_seats(event_id: str, req: ReserveRequest, db: Session = Depends(get_db)) -> dict:
    # Transactional lock would be better here, but for simplicity we use standard check
    event = db.query(EventEntity).filter(EventEntity.id == event_id).with_for_update().first()
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
    from datetime import datetime, timezone, timedelta

    current_seats = dict(event.seats)  # Copy to mutable
    expires = dict(event.reservation_expires or {})
    holders = dict(event.reservation_holder or {})
    res_ids = dict(getattr(event, "reservation_ids", {}) or {})

    def _expiry_to_ts(val):
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

    for seat_id in normalized_seats:
        if seat_id not in current_seats:
            raise HTTPException(status_code=400, detail=f"unknown seat {seat_id}")

        # Check expiry logic: support old numeric epoch or new ISO strings
        exp_val = expires.get(seat_id)
        if current_seats.get(seat_id) == "reserved" and exp_val is not None:
            exp_ts = _expiry_to_ts(exp_val)
            import time
            if exp_ts and exp_ts <= time.time():
                current_seats[seat_id] = "available"
                expires.pop(seat_id, None)
                holders.pop(seat_id, None)
                res_ids.pop(seat_id, None)

        # If seat is reserved by someone else, block
        if current_seats[seat_id] == "reserved":
            if holders.get(seat_id) != req.userId:
                raise HTTPException(status_code=409, detail=f"seat {seat_id} not available")
        elif current_seats[seat_id] != "available":
            raise HTTPException(status_code=409, detail=f"seat {seat_id} not available")

    # Reserve with a reservation id and ISO expiry
    reservation_id = str(uuid.uuid4())
    expires_at_dt = datetime.utcnow() + timedelta(seconds=state.hold_seconds)
    # ISO8601 UTC with Z
    expires_at_iso = expires_at_dt.replace(microsecond=0).isoformat() + "Z"

    for seat_id in normalized_seats:
        current_seats[seat_id] = "reserved"
        expires[seat_id] = expires_at_iso
        holders[seat_id] = req.userId
        res_ids[seat_id] = reservation_id

    # Update JSON columns
    event.seats = current_seats
    event.reservation_expires = expires
    event.reservation_holder = holders
    # attach reservation ids map (new column)
    try:
        event.reservation_ids = res_ids
    except Exception:
        # if model doesn't have attribute, ignore (compatibility)
        pass

    db.commit()

    # Publish domain event with reservation id
    assert state.broker is not None
    await state.broker.publish(
        "seat.reserved",
        {"eventId": event_id, "seats": normalized_seats, "reservationId": reservation_id},
    )

    return {"eventId": event_id, "reserved": normalized_seats, "reservationId": reservation_id, "expiresAt": expires_at_iso}


@router.post("/tickets/verify", response_model=TicketVerifyResponse)
 # Validate a per-seat ticket for a specific event and seat confirmed state.
async def verify_ticket(req: TicketVerifyRequest, db: Session = Depends(get_db)) -> TicketVerifyResponse:
    # Basic validation: ticket must exist and belong to the given event
    record = db.query(TicketEntity).filter(TicketEntity.id == req.ticketId).first()
    if record is None:
        return TicketVerifyResponse(valid=False, reason="unknown ticket")
    if record.event_id != req.eventId:
        return TicketVerifyResponse(valid=False, reason="ticket not for this event")
    # Optional: ensure seats are confirmed in the event state
    event = db.query(EventEntity).filter(EventEntity.id == req.eventId).first()
    if not event:
        return TicketVerifyResponse(valid=False, reason="event not found")
    seat_id = record.seat_id
    if event.seats.get(seat_id) != "confirmed":
        return TicketVerifyResponse(valid=False, reason="seat not confirmed")
    return TicketVerifyResponse(valid=True, ticketId=req.ticketId, eventId=req.eventId, seat=seat_id)
