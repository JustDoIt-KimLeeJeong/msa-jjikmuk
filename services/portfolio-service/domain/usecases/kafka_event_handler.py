# from app.db.engine import session
import adapters.outbound.db.sqlalchemy_query as query
from sqlalchemy.orm import Session
from datetime import datetime
from app.db.outbox_models import OrderFailureOutbox


async def order_placed(session: Session, event_type: str, dto, headers: dict):
    updated_at = await query.update_order(
        user_id=dto.user_id,
        order_id=dto.order_id,
        symbol=dto.symbol,
        qty=dto.qty,
        price=dto.price,
        side=dto.side,
        reserved_balance=dto.reserved_balance,
        reserved_qty=dto.reserved_qty,
        session=session
    )


    outbox_payload = {
        "event_id": dto.event_id,
        "occurred_at": updated_at.isoformat() if updated_at else datetime.now().isoformat(),
        "order_id": dto.order_id,
        "user_id": dto.user_id,
        "symbol": dto.symbol,
        "qty": dto.reserved_qty
    }
    
    correlation_id = headers.get("correlation_id")

    outbox_headers = {
        "correlation_id": correlation_id
    }

    await query.outbox_event_save(
        user_id=dto.user_id,
        event_type="FundsReserved",
        original_event_type=event_type,
        payload=outbox_payload,
        headers=headers,
        session=session
    )


async def trade_executed(session: Session, event_type: str, dto, headers: dict):
    order = await query.search_order(
        user_id=dto.user_id,
        order_id=dto.order_id,
        symbol=dto.symbol,
        side=dto.side,
        session=session
    )
    
    if not order:
        raise ValueError(f"Order not found: {dto.order_id}")

    if dto.side == "buy":
        await query.trade_execution_buy(
            user_id=dto.user_id,
            symbol=dto.symbol,
            price=dto.price,
            qty=dto.qty,
            session=session
        )
    elif dto.side == "sell":
        await query.trade_execution_sell(
            user_id=dto.user_id,
            symbol=dto.symbol,
            price=dto.price,
            qty=dto.qty,
            session=session
        )
    else:
        raise ValueError(f"Invalid side: {dto.side}")

    outbox_payload = {
        "event_id": dto.event_id,
        "occurred_at": datetime.now().isoformat(),
        "order_id": dto.order_id,
        "user_id": dto.user_id,
        "trade_id": dto.trade_id,
        "symbol": dto.symbol,
        "qty": dto.qty,
        "price": dto.price,
        "side": dto.side
    }

    await query.outbox_event_save(
        user_id=dto.user_id,
        event_type="PortfolioUpdated",
        payload=outbox_payload,
        headers=headers,
        session=session
    )


async def order_cancelled(session: Session, event_type: str, dto, headers: dict):
    await query.cancel_order(
        user_id=dto.user_id,
        order_id=dto.order_id,
        session=session
    )

    outbox_payload = {
        "event_id": dto.event_id,
        "occurred_at": datetime.now().isoformat(),
        "order_id": dto.order_id,
        "user_id": dto.user_id
    }

    await query.outbox_event_save(
        user_id=dto.user_id,
        event_type="FundsReleased",
        payload=outbox_payload,
        headers=headers,
        session=session
    )


async def failure_outbox_save(user_id: int, event_id: str, event_type: str, order_id: str, reason_code: str, session: Session):
    failure_event = OrderFailureOutbox(
        original_event_id=event_id,
        event_type=event_type,
        created_at=datetime.now(),
        order_id=order_id,
        user_id=user_id,
        reason_code=reason_code
    )
    session.add(failure_event)


async def test(session: Session, event_type: str, dto, headers: dict):
    print("yeap, the usecase did worked well. good for you.")

