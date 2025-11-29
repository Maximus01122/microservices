import uuid
from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.orm import Session
from app.domain.model import UserCreate, UserView, LoginRequest
from app.domain.entity import User
from app.config.database import get_db
from app import state

router = APIRouter()

@router.get("/health")
async def health() -> dict:
    return {"status": "ok"}


@router.post("/users", response_model=UserView, status_code=201)
async def register_user(req: UserCreate, db: Session = Depends(get_db)) -> UserView:
    # Enforce unique email
    if db.query(User).filter(User.email == req.email).first():
        raise HTTPException(status_code=409, detail="email already exists")

    user_id = str(uuid.uuid4())
    user = User(id=user_id, email=req.email, name=req.name)
    db.add(user)
    db.commit()
    db.refresh(user)

    assert state.broker is not None
    await state.broker.publish(
        "user.email.verification.requested",
        {"userId": user_id, "email": req.email},
    )

    return UserView(id=user.id, email=user.email, name=user.name)


@router.get("/users/{user_id}", response_model=UserView)
# Get user by id
async def get_user(user_id: str, db: Session = Depends(get_db)) -> UserView:
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="user not found")
    return UserView(id=user.id, email=user.email, name=user.name)


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
    token = f"dummy-{found.id}"
    return {"token": token, "userId": found.id}
