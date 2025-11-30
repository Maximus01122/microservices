from sqlalchemy import Column, String, Boolean, DateTime, func, text
from sqlalchemy.dialects.postgresql import UUID
from app.config.database import Base


class User(Base):
    __tablename__ = "users"

    id = Column(UUID(as_uuid=True), primary_key=True, server_default=text('uuid_generate_v4()'))
    email = Column(String(255), unique=True, index=True, nullable=False)
    name = Column(String(255))
    password_hash = Column(String(255), nullable=True)
    is_verified = Column(Boolean, nullable=False, server_default=text('false'))
    verification_token = Column(String(255), nullable=True)
    role = Column(String(50), nullable=False, server_default=text("'USER'"))
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

