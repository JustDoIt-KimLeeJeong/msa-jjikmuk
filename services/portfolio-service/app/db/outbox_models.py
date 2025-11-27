from app.db.base_models import Base
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy import (
    TIMESTAMP,
    String,
    Integer,
    Uuid, 
    Any
)
from sqlalchemy import JSON
from datetime import datetime


class Outbox(Base) : 
    __tablename__ = "outbox"
    id : Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id : Mapped[int] = mapped_column(Integer, nullable=False)
    event_type : Mapped[str] = mapped_column(String, nullable=False)
    payload : Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False)
    headers : Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False)
    created_at : Mapped[datetime] = mapped_column(TIMESTAMP, nullable=False, default=datetime.now)


    