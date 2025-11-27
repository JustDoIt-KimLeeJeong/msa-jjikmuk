from __future__ import annotations
from datetime import datetime
from decimal import Decimal
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.dialects.postgresql import JSONB
from app.db.base_models import Base

from sqlalchemy import (
    Numeric,
    TIMESTAMP,
    String,
    Integer,
    func,
    Identity,
    text,
)



class Reservation(Base):
    __tablename__ = "reservation"

    user_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    total_balance: Mapped[Decimal] = mapped_column(
        Numeric(20, 6), nullable=False, server_default=text("0")
    )
    total_reserved: Mapped[Decimal] = mapped_column(
        Numeric(20, 6), nullable=False, server_default=text("0")
    )
    updated_at: Mapped[datetime] = mapped_column(
        TIMESTAMP(timezone=True), nullable=False, server_default=func.now()
    )

class Order(Base) : 
    __tablename__ = "order"
    order_id : Mapped[str] = mapped_column(String, primary_key=True)
    user_id:  Mapped[int] = mapped_column(Integer, index=True, nullable=False)
    symbol : Mapped[str] = mapped_column(String, nullable=False)
    side : Mapped[str] = mapped_column(String, nullable=False)
    reserved_balance: Mapped[Decimal] = mapped_column(Numeric(20, 6), nullable=False, server_default=text("0"))
    reserved_qty:  Mapped[Decimal] = mapped_column(Numeric(20, 8), nullable=False, server_default=text("0"))
    updated_at:    Mapped[datetime] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now())



# balances
class Balance(Base):
    __tablename__ = "balances"

    user_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    available: Mapped[Decimal] = mapped_column(
        Numeric(20, 6), nullable=False, server_default=text("0")
    )
    reserved: Mapped[Decimal] = mapped_column(
        Numeric(20, 6), nullable=False, server_default=text("0")
    )
    updated_at: Mapped[datetime] = mapped_column(
        TIMESTAMP(timezone=True), nullable=False, server_default=func.now()
    )


# positions (composite PK) + index on updated_at DESC
class Position(Base):
    __tablename__ = "positions"

    user_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    symbol: Mapped[str] = mapped_column(String, primary_key=True)

    qty: Mapped[Decimal] = mapped_column(Numeric(20, 8), nullable=False)
    reserved_qty: Mapped[Decimal] = mapped_column(
        Numeric(20, 8), nullable=False, server_default=text("0")
    )
    avg_price: Mapped[Decimal] = mapped_column(Numeric(20, 6), nullable=False)
    realized_pnl: Mapped[Decimal] = mapped_column(
        Numeric(20, 6), nullable=False, server_default=text("0")
    )
    updated_at: Mapped[datetime] = mapped_column(
        TIMESTAMP(timezone=True), nullable=False, server_default=func.now()
    )



# processed_events (idempotency) 이게 outbox랑 겹쳐서 추가 사용 없으면 지울 것 권장
class ProcessedEvent(Base):
    __tablename__ = "processed_events"

    event_id: Mapped[str] = mapped_column(String, primary_key=True)
    occurred_at: Mapped[datetime] = mapped_column(
        TIMESTAMP(timezone=True), nullable=False
    )
    processed_at: Mapped[datetime] = mapped_column(
        TIMESTAMP(timezone=True), nullable=False, server_default=func.now()
    )


# portfolio_audit (audit log)
class PortfolioAudit(Base):
    __tablename__ = "portfolio_audit"

    id: Mapped[int] = mapped_column(
        Integer, Identity(always=True), primary_key=True
    )
    user_id: Mapped[int] = mapped_column(Integer, nullable=False)
    action: Mapped[str] = mapped_column(String, nullable=False)  # e.g., RESERVE_FUNDS
    payload: Mapped[dict] = mapped_column(JSONB, nullable=False)
    trace_id: Mapped[str | None] = mapped_column(String, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        TIMESTAMP(timezone=True), nullable=False, server_default=func.now()
    )

