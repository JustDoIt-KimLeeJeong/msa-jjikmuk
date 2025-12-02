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

    await query.outbox_event_save(
        user_id=dto.user_id,
        event_type="FundsReserved",
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


"""
async def trade_executed(session : Session, 
                         event_type : str, 
                         payload : dict, 
                         headers : dict) : 
    # 체결 성공 시 있던 order를 검색해서 다시 원복함. 
    # 로직 : 
    # 1. orderId를 확인 (이거 input시에 값이 ㅇㅆ는지 확인 필요)
    # 2. 얼마가 체결되었는지 확인. 실제 팔린 qty랑 실제로 처리된 비용을 확인 
    # 3. DB에 반영하고, 처리 완료를 발행함
    try : 
        order = query.search_order(payload.user_id, payload.order_id, payload.symbol, payload.side)
        if not order : 
            #예외 처리
            print(f"no order id {payload.order_id}")
            # 여기서 카프카 예외 이벤트 발행해줘야 함. db 롤백 해달라고 요청 해야 함, 로그 그라파나랑 로키에 띄워줘야 함.
        if  payload.side == "buy" : 
            # 산거니까 기존 order 확인해서 값을 반영 
            try :
                query.trade_execution_buy(session, payload.user_id, payload.symbol, payload.price, payload.qty)
            # 샀는거 반영 완료 퍼블리시 해줌 
            except :
                # 반영 에러 
                pass
            finally : 
                query.outbox_event_save(payload.user_id, event_type, payload, headers, session)
                # 여기서 publish로 정상 반영 되었음을 공지해줘야 함.             

        elif payload.side == "sell": 
            query.trade_execution_buy(session ,  payload.user_id,  payload.symbol, payload.price, payload.qty)
            # 산거 반영 완료 퍼블리시 필요
            
        else : 
            # 이것도 payload가 잘못된거라서 그냥 넘어가면 안되는거임. 이것도 예외처리 동일하게 이번트 발행하줘야 함. DB 롤백 필요.
            pass
        
        pass
    except : 
        pass
    finally : 
        query.outbox_event_save(payload.user_id, event_type, payload, headers, session)


async def order_cancelled(session : Session, event_type : str, payload : dict, headers : dict) :

    # 원상복귀 로직 필요함. 
    try : 
        query.cancel_order(payload.user_id , payload.order_id,session) 
        
    except :
        print("order cancell except error") 
        pass

    finally : 
        query.outbox_event_save(payload.user_id, event_type, payload, headers, session)
    


async def order_placed(session : Session, event_type : str, payload : dict, headers : dict) :
    try : 
        # print(f"payload user id is {payload.user_id}")
        updated_at = await query.update_order(payload.user_id, payload.order_id , payload.symbol, payload.qty, payload.price, payload.side, payload.reserved_balance, payload.reserved_qty, session)

        outbox_payload = {
            "event_id" : payload.event_id,
            "occucurred_at" : updated_at, 
            "order_id" : payload.order_id,
            "user_id" : payload.user_id,
            "symbol" : payload.symbol, 
            "qty" : payload.reserved_qty
        }

        await query.outbox_event_save(payload.user_id, event_type, outbox_payload, headers, session)


async def failure_outbox_save(user_id: int, event_id: str, event_type: str, order_id: str, reason_code: str, session: Session):
    failure_event = OrderFailureOutbox(
        original_event_id=event_id,
        event_type=event_type,
        occurred_at=datetime.now(),
        order_id=order_id,
        user_id=user_id,
        reason_code=reason_code
    )
    session.add(failure_event)# 테스트 코드 
    
async def test(payload : dict) : 
    print("yeap, the usecase did worked well. good for you.")


# 포트폴리오에 돈 넣는 로직 
# 포트폴리오에서 이벤트 발행하는 로직
"""