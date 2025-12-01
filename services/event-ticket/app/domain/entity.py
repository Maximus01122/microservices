from sqlalchemy import Column, String, Integer, DateTime, func, text
from sqlalchemy.dialects.postgresql import UUID, JSONB
from app.config.database import Base


class Event(Base):
    __tablename__ = "events"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text('uuid_generate_v4()'))
    name = Column(String(255), nullable=False)
    description = Column(String, nullable=True)
    venue = Column(String(1024), nullable=True)
    start_time = Column(DateTime(timezone=True), nullable=True)
    rows = Column(Integer)
    cols = Column(Integer)
    creator_user_id = Column(UUID(as_uuid=True), nullable=True)
    status = Column(String(50), nullable=False, server_default=text("'DRAFT'"))
    seats = Column(JSONB)  # seat map state (available, reserved, confirmed)
    reservation_expires = Column(JSONB, nullable=True)
    reservation_holder = Column(JSONB, nullable=True)
    reservation_ids = Column(JSONB, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)


class Ticket(Base):
    __tablename__ = "tickets"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text('uuid_generate_v4()'))
    event_id = Column(UUID(as_uuid=True), nullable=False)
    seat_id = Column(String(50))
    owner_user_id = Column(UUID(as_uuid=True), nullable=True)
    issued_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    qr_code_url = Column(String(2048), nullable=True)

