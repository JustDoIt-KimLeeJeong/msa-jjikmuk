from pydantic import BaseModel

class UserPosition(BaseModel) : 
    symbol : str
    qty : int
    avg_price: int
    realized_pnl : int

class UserBalance(BaseModel) : 
    available : int
    reserved : int


