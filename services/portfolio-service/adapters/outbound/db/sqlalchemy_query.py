from app.db.user_models import Order, Position, Balance
from app.db.outbox_models import Outbox
from sqlalchemy import select
from sqlalchemy.orm import Session
from datetime import datetime


async def search_order(user_id: int, order_id: str, symbol: str, side: str, session: Session):
    result = await session.execute(
        select(Order).where(Order.user_id == user_id, Order.order_id == order_id)
    )
    return result.scalar_one_or_none()


async def update_order(user_id: int, order_id: str, symbol: str, qty: int, price: int, side: str, reserved_balance: int, reserved_qty: int, session: Session):
    result = await session.execute(
        select(Order).where(Order.user_id == user_id, Order.order_id == order_id)
    )
    existing_order = result.scalar_one_or_none()
    
    if existing_order:
        raise ValueError(f"Order already exists: {order_id}")

    order = Order(
        order_id=order_id,
        user_id=user_id,
        symbol=symbol,
        side=side,
        reserved_balance=reserved_balance,
        reserved_qty=reserved_qty,
        updated_at=datetime.now()
    )
    session.add(order)
    return order.updated_at


async def trade_execution_buy(user_id: int, symbol: str, price: int, qty: int, session: Session):
    if qty <= 0 or price <= 0:
        raise ValueError(f"qty/price should be over 0: qty={qty}, price={price}")

    # Balance 조회 및 업데이트
    bal_result = await session.execute(
        select(Balance).where(Balance.user_id == user_id)
    )
    balance = bal_result.scalar_one_or_none()
    
    if not balance or balance.reserved < price:
        raise ValueError("Insufficient reserved balance")
    
    balance.reserved -= price
    balance.available += 0  # 필요시 로직 추가

    # Position 조회 및 업데이트
    pos_result = await session.execute(
        select(Position).where(Position.user_id == user_id, Position.symbol == symbol)
    )
    position = pos_result.scalar_one_or_none()
    
    if position:
        position.qty += qty
    else:
        # 새 포지션 생성
        new_position = Position(
            user_id=user_id,
            symbol=symbol,
            qty=qty,
            reserved_qty=0,
            avg_price=price,
            realized_pnl=0
        )
        session.add(new_position)


async def trade_execution_sell(user_id: int, symbol: str, price: int, qty: int, session: Session):
    # Balance 조회 및 업데이트
    bal_result = await session.execute(
        select(Balance).where(Balance.user_id == user_id)
    )
    balance = bal_result.scalar_one_or_none()
    
    if not balance:
        raise ValueError("Balance not found")
    
    balance.available += price

    # Position 조회 및 업데이트
    pos_result = await session.execute(
        select(Position).where(Position.user_id == user_id, Position.symbol == symbol)
    )
    position = pos_result.scalar_one_or_none()
    
    if not position or position.reserved_qty < qty:
        raise ValueError("Insufficient reserved qty")
    
    position.reserved_qty -= qty
    position.qty -= qty


async def cancel_order(user_id: int, order_id: str, session: Session):
    result = await session.execute(
        select(Order).where(Order.user_id == user_id, Order.order_id == order_id)
    )
    order = result.scalar_one_or_none()
    
    if not order:
        raise ValueError(f"Order not found: {order_id}")

    if order.side == "buy":
        bal_result = await session.execute(
            select(Balance).where(Balance.user_id == user_id)
        )
        balance = bal_result.scalar_one_or_none()
        if balance:
            balance.reserved -= order.reserved_balance
            balance.available += order.reserved_balance
            
    elif order.side == "sell":
        pos_result = await session.execute(
            select(Position).where(Position.user_id == user_id, Position.symbol == order.symbol)
        )
        position = pos_result.scalar_one_or_none()
        if position:
            position.reserved_qty -= order.reserved_qty
            position.qty += order.reserved_qty

    await session.delete(order)


async def outbox_event_save(user_id: int, event_type: str, payload: dict, headers: dict, session: Session):
    outbox = Outbox(
        user_id=user_id,
        event_type=event_type,
        payload=payload,
        headers=headers,
        created_at=datetime.now()
    )
    session.add(outbox)


async def user_portfolio(user_id: int, session: Session):
    pos_result = await session.execute(select(Position).where(Position.user_id == user_id))
    bal_result = await session.execute(select(Balance).where(Balance.user_id == user_id)
    )
    return pos_result.scalars().all(), bal_result.scalar_one_or_none()

async def update_user_balance(user_id: int, deposit: int, session: Session):
    """
    Deposit balance to user account
    
    :param user_id: user_id (type: int)
    :param deposit: money to Deposit into user account (type: int)
    :param session: Postgres Session (type: session)
    """
    bal_result = await session.execute(
        select(Balance).where(Balance.user_id == user_id)
    )
    balance = bal_result.scalar_one_or_none()
    
    if balance is None:
        balance = Balance(
            user_id=user_id,
            available=deposit,
            reserved=0,
            updated_at=datetime.now()
        )
        session.add(balance)
    else:
        balance.available += deposit
        balance.updated_at = datetime.now()
    
    return balance.available
"""

def search_order(user_id : int, order_id : str, symbol : str, side : str, session :Session) :
    # order_id로 검색! 두개가 있으면 / 없으면 에러를 띄워주는게 필요할 것 같습니당~~
    result = session.execute(select(Order).where(Order.user_id == user_id, Order.order_id == order_id))
    
    return result.scalar_one_or_none()


def trade_execution_buy(user_id : int, symbol : str, price : int, qty : int, session :Session) :
    if qty <= 0 or price <=0 :
        # 여긴 예외처리. 혹시라도 잘못 들어왔으면 안되니까...  
        raise ValueError(f"qty/price should be over 0 : qty : {qty}, price : {price}")
    
    # 예약 비용을 삭제 처리해줌. 
    # 0 이하로 내려가는 문제는 어케 방지하지 이상태면?

    
    bal = session.execute(select(Balance).where(Balance.user_id == user_id).values(reserved = Balance.reserved -price)).scalar_one()
    # 샀던 수량만큼 Position에 업데이트 
    pos = session.execute(select(Position).where(Position.user_id == user_id, Position.symbol == symbol).values(qty = Position.qty +qty)).scalar_one()
    
    if bal.reserved < price : 
        # raise error만 해두고, 실제 로직 단에서 에러 시 반영하는걸로 하장. 
        session.rollback() 
        session.close()
        raise ValueError("executed price should be under reserved price")
    

    bal.reserved -= price 
    pos.qty += qty
    
        


def trade_execution_sell(user_id : int, symbol : str, price : int, qty : int, session :Session) :
    bal = session.execute(select(Balance).where(Balance.user_id == user_id).values(reserved = Balance.reserved -price)).scalar_one()
    # 팔았던 수량만큼 Position에 업데이트 
    pos = session.execute(select(Position).where(Position.user_id == user_id, Position.symbol == symbol).values(qty = Position.qty +qty)).scalar_one()
    
    if pos.reserved_qty < qty : 
        # raise error만 해두고, 실제 로직 단에서 에러 시 반영하는걸로 하장. 
        session.rollback() 
        session.close()
        raise ValueError("executed qty should be under reserved qty")
    

    bal.reserved += price 
    pos.reserved_qty -= qty
    pass

# 새로운 order가 들어왔을 때 
async def update_order(user_id : int, order_id : str, symbol:str, qty:int, price : int, side : str,reserved_balance : int, reserved_qty : int , session :Session) :
    
    result = await session.execute(select(Order).where(Order.user_id == user_id, Order.order_id == order_id))
    order = result.scalar_one_or_none()
    # if order is not None :
    #     raise Error.OrderDuplication(user_id, order_id)

    order = Order(
        order_id = order_id, 
        user_id = user_id,
        symbol = symbol,
        side = side,
        reserved_balance = reserved_balance,
        reserved_qty = reserved_qty,
        updated_at = datetime.now()
    )
    session.add(order)
        
    return order.updated_at


def user_portfolio(user_id: int, session :Session) : 
    stmt = select(*qf.PORTFOLIO_POSITION_FIELDS).where(Position.user_id == user_id)
    position = session.execute(stmt)
    stmt = select(*qf.PORTFOLIO_BALANCE_FIELDS).where(Balance.user_id == user_id)
    balance = session.execute(stmt)
    return position.scalar_one_or_none(), balance.scalar_one_or_none()



def cancel_order(user_id : int, order_id : str, session :Session) : 
    
     
    order = session.execute(select(Order).where(Order.user_id == user_id, Order.order_id == order_id)).scalar_one()
    if order is None : 
        raise Error.OrderNotFound(user_id, order_id)
    
    if order.side == "buy" :
        # 사는거니까 balance를 돌려놓아야 함. 
        balance = session.execute(select(Balance).where(Balance.user_id == user_id, Balance.reserved == order.reserved_balance)).scalar_one()
        if balance is None : 
            raise Error.NotFound()
        balance.reserved -= order.reserved_balance
    elif order.side == "sell" :
        position = session.execute(select(Position).where(Position.user_id == user_id, Position.symbol == order.symbol)).scalar_one()
        if position is None : 
            raise Error.NotFound()
        position.reserved_qty -= order.reserved_qty
        pass
    else : 
        # 이건 side에 값이 잘못 들어간거라 에러 터져야 함. 
        pass


async def outbox_event_save(user_id:int, event_id : str, event_type : str, order_id :str, reason_code : str, session : Session) :  
    failure_event = OrderFailureOutbox(
        original_event_id = event_id,
        event_type =event_type,
        occurred_at = datetime.now(),
        order_id = order_id,
        user_id = user_id,
        reason_code=reason_code
    )

    session.add(failure_event)
"""