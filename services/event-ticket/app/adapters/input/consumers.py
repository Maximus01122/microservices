import base64
import uuid
from typing import List, Optional, Dict
import orjson
import qrcode
from io import BytesIO

from app import state
from app.domain.entity import Event as EventEntity, Ticket as TicketEntity
from app.config.database import SessionLocal

async def handle_payment_validated(payload: dict) -> None:
    # Expected payload: { orderId, eventId, seats: [..], userId }
    # Confirm seats, then emit one ticket.created per seat (with PNG QR)
    event_id = payload.get("eventId")
    seat_ids: List[str] = payload.get("seats", [])
    payer_user_id: Optional[str] = payload.get("userId")
    reservation_id: Optional[str] = payload.get("reservationId")
    order_id: Optional[str] = payload.get("orderId")
    
    db = SessionLocal()
    try:
        event = db.query(EventEntity).filter(EventEntity.id == event_id).with_for_update().first()
        if not event:
            print(f"payment for unknown event {event_id}")
            return
            
        # Confirm seats that were reserved
        current_seats = dict(event.seats)
        holders = dict(event.reservation_holder or {})
        expires = dict(event.reservation_expires or {})
        res_ids = dict(getattr(event, "reservation_ids", {}) or {})
        
        confirmed_seats: List[str] = []
        from datetime import datetime

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

        for seat_id in seat_ids:
            # Must be reserved and belong to the paying user
            if current_seats.get(seat_id) != "reserved":
                continue

            # If reservation_id provided, verify it matches
            if reservation_id:
                if res_ids.get(seat_id) != reservation_id:
                    print(f"reservation id mismatch for seat {seat_id}")
                    continue

            # Verify not expired
            exp_val = expires.get(seat_id)
            if exp_val is not None:
                exp_ts = _expiry_to_ts(exp_val)
                import time
                if exp_ts and exp_ts <= time.time():
                    print(f"reservation expired for seat {seat_id}")
                    continue

            # Verify holder matches
            if holders.get(seat_id) != payer_user_id:
                print(f"reservation holder mismatch for seat {seat_id}")
                continue

            current_seats[seat_id] = "confirmed"
            confirmed_seats.append(seat_id)
            # Clear holder, expiry and reservation id for confirmed seat
            expires.pop(seat_id, None)
            holders.pop(seat_id, None)
            res_ids.pop(seat_id, None)
        
        event.seats = current_seats
        event.reservation_expires = expires
        event.reservation_holder = holders
        try:
            event.reservation_ids = res_ids
        except Exception:
            pass
        db.commit()

        # Create one ticket per confirmed seat, each with its own QR
        assert state.broker is not None
        if confirmed_seats:
            for seat_id in confirmed_seats:
                ticket_id = str(uuid.uuid4())
                
                # Persist ticket
                ticket = TicketEntity(id=ticket_id, event_id=event_id, seat_id=seat_id)
                db.add(ticket)
                
                qr_payload = {
                    "ticketId": ticket_id,
                    "eventId": event_id,
                    "seat": seat_id,
                }
                qr_img = qrcode.make(orjson.dumps(qr_payload).decode())
                buf = BytesIO()
                qr_img.save(buf, format="PNG")
                data_url = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()

                ticket.qr_code_url = data_url

                await state.broker.publish(
                    "ticket.created",
                    {
                        "ticketId": ticket_id,
                        "eventId": event_id,
                        "seat": seat_id,
                        "qr": data_url,
                        "orderId": order_id,
                    },
                )
            
            db.commit()

            await state.broker.publish(
                "seat.confirmed",
                {"eventId": event_id, "seats": confirmed_seats},
            )
    except Exception as e:
        print(f"Error handling payment validation: {e}")
        db.rollback()
    finally:
        db.close()
