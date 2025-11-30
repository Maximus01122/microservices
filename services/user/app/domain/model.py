from typing import Dict, Optional
from datetime import datetime
from pydantic import BaseModel, Field, EmailStr


# Request body to register a new user.
class UserCreate(BaseModel):
    email: EmailStr
    name: str = Field(..., min_length=1)
    # Make password optional to tolerate frontends that don't send it yet.
    password: Optional[str] = None


# User projection returned by API calls.
class UserView(BaseModel):
    id: str
    email: EmailStr
    name: str
    is_verified: bool = False
    role: str = "USER"
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


# Login request (email + password)
class LoginRequest(BaseModel):
    email: EmailStr
    password: str


# Verification request
class VerificationRequest(BaseModel):
    token: str


# In-memory database (compat shim)
users: Dict[str, UserView] = {}

