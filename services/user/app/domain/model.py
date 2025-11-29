from typing import Dict
from pydantic import BaseModel, Field, EmailStr

# Request body to register a new user.
class UserCreate(BaseModel):
    email: EmailStr
    name: str = Field(..., min_length=1)


# User projection returned by API calls.
class UserView(BaseModel):
    id: str
    email: EmailStr
    name: str


# Mock login request (email only).
class LoginRequest(BaseModel):
    email: EmailStr

# In-memory database
users: Dict[str, UserView] = {}

