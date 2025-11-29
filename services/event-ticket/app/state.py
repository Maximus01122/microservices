import asyncio
from typing import Dict, Optional
from app.adapters.output.messaging import Broker
from app.domain.model import Event

events: Dict[str, Event] = {}
# ticketId -> { "eventId": str, "seat": str }
tickets: Dict[str, Dict[str, object]] = {}

broker: Optional[Broker] = None

# Hold duration in seconds (default 10 minutes). Can be overridden by HOLD_MINUTES env.
hold_seconds: int = 600
sweeper_task: Optional[asyncio.Task] = None

