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


# In-memory SSE streams per event_id. Each stream is an asyncio.Queue of dict payloads.
streams: Dict[str, list] = {}

async def broadcast_event(event_id: str, payload: dict) -> None:
	"""Push a payload to all connected SSE queues for the given event_id."""
	queues = list(streams.get(event_id, []))
	for q in queues:
		try:
			await q.put(payload)
		except Exception:
			# ignore queue failures; cleanup happens on disconnect
			pass

