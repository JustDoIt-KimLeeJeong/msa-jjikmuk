
from app.db.models import Order, Position, Balance
from sqlalchemy import select, update
from sqlalchemy.orm import Session
import domain.errors as Error
import datetime
import query_fields as qf


def search_order(user_id : int, order_id : str, symbol : str, side : str, session :Session) :
    # order_id로 검색! 두개가 있으면 / 없으면 에러를 띄워주는게 필요할 것 같습니당~~
    result = session.execute(select(Order).where(Order.user_id == user_id, Order.order_id == order_id))
    
    return result.scalar_one_or_none()


def trade_execution_buy(user_id : str, symbol : str, price : int, qty : int, session :Session) :
    if qty <= 0 or price <=0 :
        # 여긴 예외처리. 혹시라도 잘못 들어왔으면 안되니까...  
        raise ValueError(f"qty/price should be over 0 : qty : {qty}, price : {price}")
    
    # 예약 비용을 삭제 처리해줌. 
    # 0 이하로 내려가는 문제는 어케 방지하지 이상태면?

    with session.begin() : 
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
    
        session.commit()
        session.close()


def trade_execution_sell(user_id : str, symbol : str, price : int, qty : int, session :Session) :
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

    session.commit()
    session.close()
    pass

# 새로운 order가 들어왔을 때 
def update_order(user_id : str, order_id : str, symbol:str, qty:int, price : int, side : str,reserved_balance : int, reserved_qty : int , session :Session) :
    with session.begin() : 
        order = session.execute(select(Order).where(Order.user_id == user_id, Order.order_id == order_id)).scalar_one()
        if order is not None :
            raise Error.OrderDuplication(user_id, order_id)
    
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
        session.commit()
        session.close
    return


def user_portfolio(user_id: int, session :Session) : 
    stmt = select(*qf.PORTFOLIO_POSITION_FIELDS).where(Position.user_id == user_id)
    position = session.execute(stmt)
    stmt = select(*qf.PORTFOLIO_BALANCE_FIELDS).where(Balance.user_id == user_id)
    balance = session.execute(stmt)
    return position.scalar_one_or_none(), balance.scalar_one_or_none()



def cancel_order(user_id : int, order_id : str, session :Session) : 
    
    with session.begin() : 
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


def place_order(session  : Session) : 
    with session.begin()
    pass