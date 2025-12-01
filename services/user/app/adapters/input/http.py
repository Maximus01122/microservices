import uuid
from fastapi import APIRouter, HTTPException, Depends
from fastapi.responses import RedirectResponse
from sqlalchemy.orm import Session
from app.domain.model import UserCreate, UserView, LoginRequest
from app.domain.entity import User
from app.config.database import get_db
from app import state
from passlib.context import CryptContext

router = APIRouter()

# Password hashing context (bcrypt)
# Use PBKDF2-SHA256 for password hashing to avoid bcrypt binary dependency issues
pwd_context = CryptContext(schemes=["pbkdf2_sha256"], deprecated="auto")

@router.get("/health")
async def health() -> dict:
    return {"status": "ok"}


@router.post("/users", response_model=UserView, status_code=201)
async def register_user(req: UserCreate, db: Session = Depends(get_db)) -> UserView:
    # Enforce unique email
    if db.query(User).filter(User.email == req.email).first():
        raise HTTPException(status_code=409, detail="email already exists")

    user_id = str(uuid.uuid4())
    verification_token = str(uuid.uuid4())
    # Hash password if provided
    password_hash = None
    if req.password:
        password_hash = pwd_context.hash(req.password)

    user = User(id=user_id, email=req.email, name=req.name, password_hash=password_hash, verification_token=verification_token)
    db.add(user)
    db.commit()
    db.refresh(user)

    assert state.broker is not None
    await state.broker.publish(
        "user.email.verification.requested",
        {"userId": user_id, "email": req.email, "token": verification_token},
    )

    return UserView(id=str(user.id), email=user.email, name=user.name, is_verified=user.is_verified, role=user.role, created_at=user.created_at, updated_at=user.updated_at)


@router.get("/users/{user_id}", response_model=UserView)
# Get user by id
async def get_user(user_id: str, db: Session = Depends(get_db)) -> UserView:
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="user not found")
    return UserView(id=str(user.id), email=user.email, name=user.name)


@router.delete("/users/{user_id}", status_code=204)
# Delete user by id
async def delete_user(user_id: str, db: Session = Depends(get_db)) -> None:
    user = db.query(User).filter(User.id == user_id).first()
    if user:
        db.delete(user)
        db.commit()
        return
    raise HTTPException(status_code=404, detail="user not found")

@router.post("/login")
# Mock login: find by email and return a dummy token
async def login(req: LoginRequest, db: Session = Depends(get_db)) -> dict:
    # Mock login: find by email and return a dummy token
    found = db.query(User).filter(User.email == req.email).first()
    if not found:
        raise HTTPException(status_code=401, detail="invalid credentials")

    # Prevent login if user hasn't verified their email
    if not found.is_verified:
        raise HTTPException(status_code=403, detail="email not verified")

    # Validate password against stored hash
    if not found.password_hash or not pwd_context.verify(req.password, found.password_hash):
        raise HTTPException(status_code=401, detail="invalid credentials")

    token = f"dummy-{found.id}"
    return {"token": token, "userId": str(found.id)}


@router.get("/verify")
async def verify_user(token: str, db: Session = Depends(get_db)) -> RedirectResponse:
    """Verify a user by token. If successful, mark user as verified and redirect to the frontend homepage."""
    user = db.query(User).filter(User.verification_token == token).first()
    if not user:
        raise HTTPException(status_code=404, detail="invalid or expired token")

    user.is_verified = True
    user.verification_token = None
    db.add(user)
    db.commit()

    # Redirect the user to the frontend (homepage). The frontend runs on port 3000 in compose.
    return RedirectResponse(url="http://localhost:3000/?verified=true")
