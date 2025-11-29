from sqlalchemy import Column, String, Integer, JSON
from app.config.database import Base

class Event(Base):
    __tablename__ = "events"

    id = Column(String, primary_key=True, index=True)
    name = Column(String)
    rows = Column(Integer)
    cols = Column(Integer)
    creator_user_id = Column(String)
    seats = Column(JSON) # Storing seats as JSON for simplicity in this migration
    reservation_expires = Column(JSON, nullable=True)
    reservation_holder = Column(JSON, nullable=True)

class Ticket(Base):
    __tablename__ = "tickets"

    id = Column(String, primary_key=True, index=True)
    event_id = Column(String)
    seat_id = Column(String)

