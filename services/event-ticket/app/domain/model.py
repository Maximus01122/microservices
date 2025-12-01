import string
import time
from typing import Dict, List, Optional
from datetime import datetime
from pydantic import BaseModel, Field

# --------------------
# DTOs (API contracts)
# --------------------

# Request body to create a new event with a grid seat map.
class EventCreate(BaseModel):
    name: str = Field(..., min_length=1)
    rows: int = Field(..., ge=1, le=26)  # up to Z
    cols: int = Field(..., ge=1, le=50)
    userId: str = Field(..., min_length=1)
    basePriceCents: int = Field(0, ge=0)
    description: Optional[str] = None
    venue: Optional[str] = None
    start_time: Optional[datetime] = None


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
    description: Optional[str] = None
    venue: Optional[str] = None
    start_time: Optional[datetime] = None
    creator_user_id: Optional[str] = None
    status: str
    seats: Dict[str, str]  # seatId -> status: available|reserved|confirmed
    basePriceCents: int = 0
    seatPrices: Dict[str, int] = {}
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


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


class TicketView(BaseModel):
    id: str
    event_id: str
    seat_id: str
    owner_user_id: Optional[str] = None
    issued_at: Optional[datetime] = None
    qr_code_url: Optional[str] = None


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

# Release a seat if its reservation has expired; return True if released
def release_if_expired(event: Event, seat_id: str) -> bool:
    exp = getattr(event, "reservation_expires", {}).get(seat_id)
    if event.seats.get(seat_id) == "reserved" and exp is not None and exp <= time.time():
        event.seats[seat_id] = "available"
        event.reservation_expires.pop(seat_id, None)
        return True
    return False

