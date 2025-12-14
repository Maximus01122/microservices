"""
Unit tests for Event-Ticket Service.
Tests cover event creation, listing, reservations, and ticket validation.
Uses mocking to avoid PostgreSQL-specific UUID type issues.
"""
import pytest
from unittest.mock import MagicMock, AsyncMock
from fastapi.testclient import TestClient
from datetime import datetime, timezone

from app.main import app
from app.config.database import get_db
from app import state


def create_mock_event(
    event_id="event-123",
    name="Test Concert",
    rows=5,
    cols=10,
    user_id="user-1",
    base_price_cents=5000
):
    """Create a mock event object with all required fields."""
    # Generate seats as a dict (not JSON string - JSONB auto-deserializes)
    seats = {}
    for r in range(rows):
        row_letter = chr(ord('A') + r)
        for c in range(1, cols + 1):
            seat_id = f"{row_letter}{c}"
            seats[seat_id] = "available"
    
    event = MagicMock()
    event.id = event_id
    event.name = name
    event.rows = rows
    event.cols = cols
    event.seats = seats  # Dict, not JSON string
    event.seat_prices = {}
    event.base_price_cents = base_price_cents
    event.user_id = user_id
    event.creator_user_id = user_id
    event.description = None  # Optional field
    event.venue = None  # Optional field
    event.start_time = None  # Optional field
    event.status = "DRAFT"
    event.reservation_expires = {}
    event.reservation_holder = {}
    event.reservation_ids = {}
    event.created_at = datetime.now(timezone.utc)
    event.updated_at = datetime.now(timezone.utc)
    return event


def create_mock_ticket(
    ticket_id="ticket-123",
    event_id="event-123",
    seat_id="A1",
    qr_code_url="data:image/png;base64,..."
):
    """Create a mock ticket object."""
    ticket = MagicMock()
    ticket.id = ticket_id
    ticket.event_id = event_id
    ticket.seat_id = seat_id
    ticket.qr_code_url = qr_code_url
    ticket.owner_user_id = None
    ticket.issued_at = datetime.now(timezone.utc)
    return ticket


@pytest.fixture
def mock_db():
    """Create a mock database session with proper chaining."""
    db = MagicMock()
    db.query.return_value.filter.return_value.first.return_value = None
    db.query.return_value.filter.return_value.with_for_update.return_value.first.return_value = None
    db.query.return_value.all.return_value = []
    db.query.return_value.order_by.return_value.all.return_value = []
    db.add = MagicMock()
    db.commit = MagicMock()
    db.refresh = MagicMock()
    db.delete = MagicMock()
    return db


@pytest.fixture
def mock_broker():
    """Mock the RabbitMQ broker."""
    broker = AsyncMock()
    broker.publish = AsyncMock()
    original_broker = state.broker
    state.broker = broker
    yield broker
    state.broker = original_broker


@pytest.fixture
def client(mock_db):
    """Create a test client with mocked DB."""
    def override_get_db():
        yield mock_db
    
    app.dependency_overrides[get_db] = override_get_db
    yield TestClient(app)
    app.dependency_overrides.clear()


class TestHealthEndpoint:
    """Tests for the health check endpoint."""

    def test_health_returns_ok(self, client):
        """Health endpoint should return status ok."""
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json() == {"status": "ok"}


class TestEventCreation:
    """Tests for event creation."""

    def test_create_event_success(self, mock_db, mock_broker):
        """Successfully create a new event."""
        mock_db.query.return_value.filter.return_value.first.return_value = None
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/events",
            json={
                "name": "Test Concert",
                "rows": 5,
                "cols": 10,
                "basePriceCents": 5000,
                "userId": "user-123"
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 201
        data = response.json()
        assert data["name"] == "Test Concert"
        assert data["rows"] == 5
        assert data["cols"] == 10
        assert data["basePriceCents"] == 5000
        assert "id" in data
        assert mock_db.add.called
        assert mock_db.commit.called

    def test_create_event_generates_seats(self, mock_db, mock_broker):
        """Event creation should generate seat map."""
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/events",
            json={
                "name": "Seated Event",
                "rows": 3,
                "cols": 4,
                "basePriceCents": 2500,
                "userId": "user-456"
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 201
        data = response.json()
        # Should have 3 rows * 4 cols = 12 seats
        assert "seats" in data
        seats = data["seats"]
        assert len(seats) == 12
        # Verify seat naming (A1, A2, ... B1, B2, etc.)
        assert "A1" in seats
        assert "C4" in seats

    def test_create_event_invalid_rows_fails(self, client):
        """Event creation with invalid rows should fail."""
        response = client.post(
            "/events",
            json={
                "name": "Bad Event",
                "rows": 0,
                "cols": 10,
                "basePriceCents": 1000,
                "userId": "user-1"
            }
        )
        assert response.status_code == 422


class TestEventListing:
    """Tests for listing events."""

    def test_list_events_empty(self, mock_db):
        """Listing events when none exist returns empty list."""
        mock_db.query.return_value.order_by.return_value.all.return_value = []
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.get("/events")
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 200
        assert response.json() == []

    def test_list_events_returns_all(self, mock_db):
        """Listing events returns all created events."""
        event1 = create_mock_event(event_id="e1", name="Event 1", rows=2, cols=2)
        event2 = create_mock_event(event_id="e2", name="Event 2", rows=2, cols=2)
        mock_db.query.return_value.order_by.return_value.all.return_value = [event1, event2]
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.get("/events")
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 200
        events = response.json()
        assert len(events) == 2
        names = [e["name"] for e in events]
        assert "Event 1" in names
        assert "Event 2" in names


class TestGetEvent:
    """Tests for getting a single event."""

    def test_get_event_success(self, mock_db):
        """Successfully get event by ID."""
        event = create_mock_event(event_id="fetch-me", name="Fetch Me", rows=2, cols=2)
        mock_db.query.return_value.filter.return_value.first.return_value = event
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.get("/events/fetch-me")
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 200
        data = response.json()
        assert data["name"] == "Fetch Me"
        assert data["id"] == "fetch-me"

    def test_get_nonexistent_event_fails(self, mock_db):
        """Getting non-existent event should return 404."""
        mock_db.query.return_value.filter.return_value.first.return_value = None
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.get("/events/nonexistent-id")
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 404


class TestReservations:
    """Tests for seat reservations."""

    def test_create_reservation_success(self, mock_db, mock_broker):
        """Successfully reserve seats."""
        event = create_mock_event(event_id="reserve-event", rows=3, cols=3)
        # Need to set up with_for_update chain
        mock_db.query.return_value.filter.return_value.with_for_update.return_value.first.return_value = event
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/events/reserve-event/reservations",
            json={
                "userId": "buyer-123",
                "seats": ["A1", "A2"]
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 201
        data = response.json()
        assert "reservationId" in data
        assert set(data["reserved"]) == {"A1", "A2"}
        assert "expiresAt" in data

    def test_reserve_invalid_seat_fails(self, mock_db, mock_broker):
        """Reserving a non-existent seat should fail."""
        event = create_mock_event(event_id="small-event", rows=2, cols=2)
        mock_db.query.return_value.filter.return_value.with_for_update.return_value.first.return_value = event
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/events/small-event/reservations",
            json={
                "userId": "buyer-1",
                "seats": ["Z99"]
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 400

    def test_reserve_nonexistent_event_fails(self, mock_db, mock_broker):
        """Reserving seats for non-existent event should fail."""
        mock_db.query.return_value.filter.return_value.with_for_update.return_value.first.return_value = None
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/events/fake-event/reservations",
            json={
                "userId": "buyer-1",
                "seats": ["A1"]
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 404


class TestTicketValidation:
    """Tests for ticket validation."""

    def test_validate_ticket_success(self, mock_db):
        """Successfully validate a ticket (seat must be confirmed)."""
        # Create event with confirmed seat
        event = create_mock_event(event_id="event-123", rows=2, cols=2)
        event.seats["A1"] = "confirmed"  # Must be confirmed for valid ticket
        
        ticket = create_mock_ticket(
            ticket_id="valid-ticket",
            event_id="event-123",
            seat_id="A1"
        )
        
        # First query returns ticket, second returns event
        def mock_query_side_effect(entity):
            mock_result = MagicMock()
            if "Ticket" in str(entity):
                mock_result.filter.return_value.first.return_value = ticket
            else:
                mock_result.filter.return_value.first.return_value = event
            return mock_result
        
        mock_db.query.side_effect = mock_query_side_effect
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/ticket-validations",
            json={
                "ticketId": "valid-ticket",
                "eventId": "event-123"
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 200
        data = response.json()
        assert data["valid"] is True
        assert data["ticketId"] == "valid-ticket"
        assert data["seat"] == "A1"

    def test_validate_invalid_ticket_returns_valid_false(self, mock_db):
        """Validating non-existent ticket returns valid=False (not 404)."""
        mock_db.query.return_value.filter.return_value.first.return_value = None
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/ticket-validations",
            json={
                "ticketId": "fake-ticket",
                "eventId": "event-123"
            }
        )
        
        app.dependency_overrides.clear()
        
        # API returns 200 with valid=False, not 404
        assert response.status_code == 200
        data = response.json()
        assert data["valid"] is False
        assert "unknown" in data["reason"].lower()

    def test_validate_ticket_wrong_event_returns_valid_false(self, mock_db):
        """Validating ticket for wrong event returns valid=False."""
        ticket = create_mock_ticket(
            ticket_id="ticket-123",
            event_id="event-123",  # Ticket is for event-123
            seat_id="A1"
        )
        mock_db.query.return_value.filter.return_value.first.return_value = ticket
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/ticket-validations",
            json={
                "ticketId": "ticket-123",
                "eventId": "different-event"  # Wrong event
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 200
        data = response.json()
        assert data["valid"] is False
