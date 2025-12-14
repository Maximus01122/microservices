"""
Unit tests for User Service.
Tests cover user registration, login, retrieval, and deletion.
Uses mocking to avoid PostgreSQL-specific UUID type issues.
"""
import pytest
from unittest.mock import MagicMock, AsyncMock
from fastapi.testclient import TestClient
from datetime import datetime, timezone

from app.main import app
from app.config.database import get_db
from app import state


def create_mock_user(
    user_id="test-user-id",
    email="test@example.com",
    name="Test User",
    password_hash="$pbkdf2-sha256$29000$hashed",
    is_verified=True,
    verification_token=None,
    role="user"
):
    """Create a mock user object."""
    user = MagicMock()
    user.id = user_id
    user.email = email
    user.name = name
    user.password_hash = password_hash
    user.is_verified = is_verified
    user.verification_token = verification_token
    user.role = role
    user.created_at = datetime.now(timezone.utc)
    user.updated_at = datetime.now(timezone.utc)
    return user


@pytest.fixture
def mock_db():
    """Create a mock database session."""
    db = MagicMock()
    db.query.return_value.filter.return_value.first.return_value = None
    db.add = MagicMock()
    db.commit = MagicMock()
    db.delete = MagicMock()
    
    # Configure refresh to set default values on the user object
    def mock_refresh(obj):
        if not hasattr(obj, 'is_verified') or obj.is_verified is None:
            obj.is_verified = False
        if not hasattr(obj, 'role') or obj.role is None:
            obj.role = "user"
        if not hasattr(obj, 'created_at') or obj.created_at is None:
            obj.created_at = datetime.now(timezone.utc)
        if not hasattr(obj, 'updated_at') or obj.updated_at is None:
            obj.updated_at = datetime.now(timezone.utc)
    
    db.refresh = mock_refresh
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


class TestUserRegistration:
    """Tests for user registration."""

    def test_register_user_success(self, mock_db, mock_broker):
        """Successfully register a new user."""
        # Setup: No existing user with this email
        mock_db.query.return_value.filter.return_value.first.return_value = None
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/users",
            json={
                "email": "newuser@example.com",
                "name": "New User",
                "password": "SecurePass123!"
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 201
        data = response.json()
        assert data["email"] == "newuser@example.com"
        assert data["name"] == "New User"
        assert "id" in data
        assert mock_db.add.called
        assert mock_db.commit.called

    def test_register_duplicate_email_fails(self, mock_db, mock_broker):
        """Registration with existing email should fail with 409."""
        # Setup: User already exists
        existing_user = create_mock_user(email="existing@example.com")
        mock_db.query.return_value.filter.return_value.first.return_value = existing_user
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/users",
            json={
                "email": "existing@example.com",
                "name": "Duplicate User",
                "password": "SecurePass123!"
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 409
        assert "already" in response.json()["detail"].lower()

    def test_register_invalid_email_fails(self, client):
        """Registration with invalid email should fail."""
        response = client.post(
            "/users",
            json={
                "email": "not-an-email",
                "name": "Test User",
                "password": "SecurePass123!"
            }
        )
        assert response.status_code == 422

    def test_register_empty_name_fails(self, client):
        """Registration with empty name should fail."""
        response = client.post(
            "/users",
            json={
                "email": "test@example.com",
                "name": "",
                "password": "SecurePass123!"
            }
        )
        assert response.status_code == 422


class TestUserLogin:
    """Tests for user login."""

    def test_login_success(self, mock_db, mock_broker):
        """Successfully login with correct credentials."""
        from passlib.context import CryptContext
        pwd_context = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")
        
        # Create user with known password hash
        password = "SecurePass123!"
        hashed = pwd_context.hash(password)
        verified_user = create_mock_user(
            user_id="user-123",
            email="login@example.com",
            password_hash=hashed,
            is_verified=True
        )
        mock_db.query.return_value.filter.return_value.first.return_value = verified_user
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/sessions",
            json={
                "email": "login@example.com",
                "password": password
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 200
        data = response.json()
        assert "token" in data
        assert data["userId"] == "user-123"

    def test_login_wrong_password_fails(self, mock_db):
        """Login with wrong password should fail."""
        from passlib.context import CryptContext
        pwd_context = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")
        
        # User with different password
        verified_user = create_mock_user(
            email="wrongpass@example.com",
            password_hash=pwd_context.hash("CorrectPassword"),
            is_verified=True
        )
        mock_db.query.return_value.filter.return_value.first.return_value = verified_user
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/sessions",
            json={
                "email": "wrongpass@example.com",
                "password": "WrongPassword123!"
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 401
        assert "invalid" in response.json()["detail"].lower()

    def test_login_unverified_user_fails(self, mock_db):
        """Login without email verification should fail."""
        from passlib.context import CryptContext
        pwd_context = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")
        
        password = "SecurePass123!"
        unverified_user = create_mock_user(
            email="unverified@example.com",
            password_hash=pwd_context.hash(password),
            is_verified=False
        )
        mock_db.query.return_value.filter.return_value.first.return_value = unverified_user
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/sessions",
            json={
                "email": "unverified@example.com",
                "password": password
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 403
        # Check for "verified" instead of "verify"
        assert "verified" in response.json()["detail"].lower()

    def test_login_nonexistent_user_fails(self, mock_db):
        """Login with non-existent email should fail."""
        mock_db.query.return_value.filter.return_value.first.return_value = None
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.post(
            "/sessions",
            json={
                "email": "nonexistent@example.com",
                "password": "AnyPassword123!"
            }
        )
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 401


class TestGetUser:
    """Tests for getting user by ID."""

    def test_get_user_success(self, mock_db):
        """Successfully get user by ID."""
        user = create_mock_user(
            user_id="user-456",
            email="getuser@example.com",
            name="Get User"
        )
        mock_db.query.return_value.filter.return_value.first.return_value = user
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.get("/users/user-456")
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 200
        data = response.json()
        assert data["email"] == "getuser@example.com"
        assert data["name"] == "Get User"

    def test_get_nonexistent_user_fails(self, mock_db):
        """Getting non-existent user should return 404."""
        mock_db.query.return_value.filter.return_value.first.return_value = None
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.get("/users/nonexistent-id")
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 404


class TestDeleteUser:
    """Tests for user deletion."""

    def test_delete_user_success(self, mock_db):
        """Successfully delete a user (returns 204 No Content)."""
        user = create_mock_user(user_id="delete-user-id")
        mock_db.query.return_value.filter.return_value.first.return_value = user
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.delete("/users/delete-user-id")
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 204
        assert mock_db.delete.called
        assert mock_db.commit.called

    def test_delete_nonexistent_user_fails(self, mock_db):
        """Deleting non-existent user should return 404."""
        mock_db.query.return_value.filter.return_value.first.return_value = None
        
        def override_get_db():
            yield mock_db
        
        app.dependency_overrides[get_db] = override_get_db
        client = TestClient(app)
        
        response = client.delete("/users/nonexistent-id")
        
        app.dependency_overrides.clear()
        
        assert response.status_code == 404
