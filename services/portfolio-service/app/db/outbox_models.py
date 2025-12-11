from app.db.base_models import Base
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy import (
    TIMESTAMP,
    String,
    Integer,
    Uuid, 
    Any
)
from sqlalchemy.dialects.postgresql import JSONB
from datetime import datetime


class Outbox(Base) : 
    __tablename__ = "outbox"
    id : Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    user_id : Mapped[int] = mapped_column(Integer, nullable=False)
    event_type : Mapped[str] = mapped_column(String, nullable=False)
    payload : Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False)
    headers : Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False)
    created_at : Mapped[datetime] = mapped_column(TIMESTAMP, nullable=False, default=datetime.now)



class OrderFailureOutbox(Base) : 
    __tablename__ = "order_failure_outbox"
    id : Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True) # 저장된 이벤트 id 부여 
    original_event_id : Mapped[str] = mapped_column(String, nullable=False) # consumer가 받은 이벤트 id
    event_type : Mapped[str] = mapped_column(String, nullable=False) # Router가 실행했던 이벤트 이름(consumer가 수신한 이벤트 이름 ) 
    created_at : Mapped[datetime] = mapped_column(TIMESTAMP, nullable=False, default=datetime.now)
    user_id : Mapped[int] = mapped_column(Integer, nullable=False)
    reason_code : Mapped[str] = mapped_column(String, nullable=False)

    order_id : Mapped[str] = mapped_column(String, nullable=True)
    symbol : Mapped[str] = mapped_column(String, nullable=True)
    qty : Mapped[int] = mapped_column(Integer, nullable=True)
    

class InternalFailureOutbox(Base) : 
    __tablename__ = "internal_failure_outbox"
    id : Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True) # 저장된 이벤트 id 부여 
    user_id : Mapped[int] = mapped_column(Integer, nullable=False)
    occured_at : Mapped[datetime] = mapped_column(TIMESTAMP, nullable=False, default=datetime.now)
    reason_code : Mapped[str] = mapped_column(String, nullable=False)

    symbol : Mapped[str] = mapped_column(String, nullable=True)
    qty : Mapped[int] = mapped_column(Integer, nullable=True)
    

