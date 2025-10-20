import adapters.outbound.db.sqlalchemy_query as query
from sqlalchemy.orm import Session

def get_portfolio(user_id : int, session : Session):
    position, balance = query.user_portfolio(session, user_id)
    return {"user_id" : user_id, "position" : position, "balance" : balance} 


