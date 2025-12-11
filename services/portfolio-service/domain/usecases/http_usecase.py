import adapters.outbound.db.sqlalchemy_query as query
from sqlalchemy.orm import Session

async def get_portfolio(user_id : int, session : Session):
    """
    Docstring for get_portfolio
    
    :param user_id: user Id (type : int)
    :param session: DB Session for Http (type : Session)
    :return: Current User Portfloio 
    """
    position, balance = await query.user_portfolio(user_id, session)
    return {"user_id" : user_id, "position" : position, "balance" : balance} 


async def update_balance(user_id: int, deposit: int, session: Session):
    """
    Deposit money(balance) into User trade account
    
    :param user_id: user Id (type : int)
    :param deposit: Deposit money to user trade account (type : int)
    :param session: DB Session for Http (type : Session)
    :return: updated balance
    """
    new_balance = await query.update_user_balance(user_id, deposit, session)
    return {"user_id": user_id, "available_balance": new_balance}