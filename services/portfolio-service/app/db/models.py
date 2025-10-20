from __future__ import annotations
from datetime import datetime
from decimal import Decimal
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy.dialects.postgresql import JSONB

from sqlalchemy import (
    Numeric,
    TIMESTAMP,
    String,
    BigInteger,
    Index,
    func,
    Identity,
    text,
)


class Base(DeclarativeBase):
    pass


class Reservation(Base):
    __tablename__ = "reservation"

    user_id: Mapped[str] = mapped_column(String, primary_key=True)
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
    user_id:  Mapped[str] = mapped_column(String, index=True, nullable=False)
    symbol : Mapped[str] = mapped_column(String, nullable=False)
    side : Mapped[str] = mapped_column(String, nullable=False)
    reserved_balance: Mapped[Decimal] = mapped_column(Numeric(20, 6), nullable=False, server_default=text("0"))
    reserved_qty:  Mapped[Decimal] = mapped_column(Numeric(20, 8), nullable=False, server_default=text("0"))
    updated_at:    Mapped[datetime] = mapped_column(TIMESTAMP(timezone=True), nullable=False, server_default=func.now())



# balances
class Balance(Base):
    __tablename__ = "balances"

    user_id: Mapped[str] = mapped_column(String, primary_key=True)
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

    user_id: Mapped[str] = mapped_column(String, primary_key=True)
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

    # __table_args__ = (
    #     # PostgreSQL에서 정렬 지정 인덱스
    #     Index("idx_positions_updated_at_desc", updated_at.desc()),
    # )


# processed_events (idempotency)
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
        BigInteger, Identity(always=True), primary_key=True
    )
    user_id: Mapped[str] = mapped_column(String, nullable=False)
    action: Mapped[str] = mapped_column(String, nullable=False)  # e.g., RESERVE_FUNDS
    payload: Mapped[dict] = mapped_column(JSONB, nullable=False)
    trace_id: Mapped[str | None] = mapped_column(String, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        TIMESTAMP(timezone=True), nullable=False, server_default=func.now()
    )


